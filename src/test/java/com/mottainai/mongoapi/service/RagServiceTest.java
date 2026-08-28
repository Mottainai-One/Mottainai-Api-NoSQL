package com.mottainai.mongoapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.mottainai.mongoapi.document.RagChunk;
import com.mottainai.mongoapi.document.RagDocument;
import com.mottainai.mongoapi.dto.rag.RagChunkRequest;
import com.mottainai.mongoapi.dto.rag.RagChunkResponse;
import com.mottainai.mongoapi.dto.rag.RagDocumentRequest;
import com.mottainai.mongoapi.dto.rag.RagDocumentResponse;
import com.mottainai.mongoapi.exception.DuplicateSlugException;
import com.mottainai.mongoapi.exception.ResourceNotFoundException;
import com.mottainai.mongoapi.mapper.RagMapper;
import com.mottainai.mongoapi.repository.RagChunkRepository;
import com.mottainai.mongoapi.repository.RagDocumentRepository;

import org.mapstruct.factory.Mappers;

@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    private static final Long EMPRESA_ID = 1L;
    private static final Long OTHER_EMPRESA_ID = 2L;

    @Mock
    private RagDocumentRepository ragDocumentRepository;

    @Mock
    private RagChunkRepository ragChunkRepository;

    private RagService ragService;

    @BeforeEach
    void setUp() {
        RagMapper mapper = Mappers.getMapper(RagMapper.class);
        ragService = new RagService(ragDocumentRepository, ragChunkRepository, mapper);
    }

    private RagDocument sampleDocument(String id, Long empresaId, String slug) {
        return RagDocument.builder().id(id).empresaId(empresaId).slug(slug)
                .title("Título").source("manual_operacional").build();
    }

    @Test
    void createDocument_whenSlugUnique_savesDocument() {
        RagDocumentRequest request = new RagDocumentRequest("novo-slug", EMPRESA_ID, "Título", "manual_operacional");
        when(ragDocumentRepository.findBySlug("novo-slug")).thenReturn(Optional.empty());
        when(ragDocumentRepository.save(any(RagDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RagDocumentResponse response = ragService.createDocument(request);

        assertThat(response.slug()).isEqualTo("novo-slug");
        assertThat(response.empresaId()).isEqualTo(EMPRESA_ID);
    }

    @Test
    void createDocument_whenSlugDuplicate_throwsDuplicateSlugException() {
        RagDocumentRequest request = new RagDocumentRequest("existente", EMPRESA_ID, "Título", "manual_operacional");
        when(ragDocumentRepository.findBySlug("existente"))
                .thenReturn(Optional.of(sampleDocument("id-1", EMPRESA_ID, "existente")));

        assertThatThrownBy(() -> ragService.createDocument(request))
                .isInstanceOf(DuplicateSlugException.class);

        verify(ragDocumentRepository, never()).save(any());
    }

    @Test
    void getDocument_whenBelongsToAnotherEmpresa_throwsNotFound() {
        when(ragDocumentRepository.findByIdAndEmpresaId("id-1", OTHER_EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ragService.getDocument("id-1", OTHER_EMPRESA_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteDocument_cascadesToChunks() {
        RagDocument document = sampleDocument("id-1", EMPRESA_ID, "slug-1");
        when(ragDocumentRepository.findByIdAndEmpresaId("id-1", EMPRESA_ID)).thenReturn(Optional.of(document));

        ragService.deleteDocument("id-1", EMPRESA_ID);

        verify(ragChunkRepository, times(1)).deleteByDocumentId("id-1");
        verify(ragDocumentRepository, times(1)).delete(document);
    }

    @Test
    void deleteDocument_whenNotOwned_doesNotTouchChunks() {
        when(ragDocumentRepository.findByIdAndEmpresaId("id-1", OTHER_EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ragService.deleteDocument("id-1", OTHER_EMPRESA_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(ragChunkRepository, never()).deleteByDocumentId(any());
        verify(ragDocumentRepository, never()).delete(any(RagDocument.class));
    }

    @Test
    void updateDocument_whenNewSlugUsedByAnotherDocument_throwsDuplicateSlugException() {
        RagDocument document = sampleDocument("id-1", EMPRESA_ID, "slug-atual");
        RagDocument other = sampleDocument("id-2", EMPRESA_ID, "slug-alvo");
        RagDocumentRequest request = new RagDocumentRequest("slug-alvo", EMPRESA_ID, "Novo título", "faq");

        when(ragDocumentRepository.findByIdAndEmpresaId("id-1", EMPRESA_ID)).thenReturn(Optional.of(document));
        when(ragDocumentRepository.findBySlug("slug-alvo")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> ragService.updateDocument("id-1", EMPRESA_ID, request))
                .isInstanceOf(DuplicateSlugException.class);
    }

    @Test
    void updateDocument_whenSlugBelongsToItself_updatesNormally() {
        RagDocument document = sampleDocument("id-1", EMPRESA_ID, "slug-atual");
        RagDocumentRequest request = new RagDocumentRequest("slug-atual", EMPRESA_ID, "Novo título", "faq");

        when(ragDocumentRepository.findByIdAndEmpresaId("id-1", EMPRESA_ID)).thenReturn(Optional.of(document));
        when(ragDocumentRepository.findBySlug("slug-atual")).thenReturn(Optional.of(document));
        when(ragDocumentRepository.save(any(RagDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RagDocumentResponse response = ragService.updateDocument("id-1", EMPRESA_ID, request);

        assertThat(response.title()).isEqualTo("Novo título");
        assertThat(response.source()).isEqualTo("faq");
    }

    @Test
    void createChunk_whenChunkNumberNotProvided_usesMaxPlusOne() {
        RagDocument document = sampleDocument("doc-1", EMPRESA_ID, "slug-1");
        RagChunkRequest request = new RagChunkRequest(null, "texto do novo chunk");

        when(ragDocumentRepository.findByIdAndEmpresaId("doc-1", EMPRESA_ID)).thenReturn(Optional.of(document));
        when(ragChunkRepository.findTopByDocumentIdOrderByChunkDesc("doc-1"))
                .thenReturn(Optional.of(RagChunk.builder().documentId("doc-1").chunk(4).build()));
        when(ragChunkRepository.save(any(RagChunk.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RagChunkResponse response = ragService.createChunk("doc-1", EMPRESA_ID, request);

        assertThat(response.chunk()).isEqualTo(5);
    }

    @Test
    void createChunk_whenDocumentHasNoChunksYet_startsAtZero() {
        RagDocument document = sampleDocument("doc-1", EMPRESA_ID, "slug-1");
        RagChunkRequest request = new RagChunkRequest(null, "primeiro chunk");

        when(ragDocumentRepository.findByIdAndEmpresaId("doc-1", EMPRESA_ID)).thenReturn(Optional.of(document));
        when(ragChunkRepository.findTopByDocumentIdOrderByChunkDesc("doc-1")).thenReturn(Optional.empty());
        when(ragChunkRepository.save(any(RagChunk.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RagChunkResponse response = ragService.createChunk("doc-1", EMPRESA_ID, request);

        assertThat(response.chunk()).isEqualTo(0);
    }

    @Test
    void createChunk_whenChunkNumberProvided_usesProvidedValueAndNeverPersistsEmbedding() {
        RagDocument document = sampleDocument("doc-1", EMPRESA_ID, "slug-1");
        RagChunkRequest request = new RagChunkRequest(10, "texto explícito");

        when(ragDocumentRepository.findByIdAndEmpresaId("doc-1", EMPRESA_ID)).thenReturn(Optional.of(document));
        ArgumentCaptor<RagChunk> captor = ArgumentCaptor.forClass(RagChunk.class);
        when(ragChunkRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        ragService.createChunk("doc-1", EMPRESA_ID, request);

        verify(ragChunkRepository, never()).findTopByDocumentIdOrderByChunkDesc(any());
        RagChunk saved = captor.getValue();
        assertThat(saved.getChunk()).isEqualTo(10);
        assertThat(saved.getEmbedding()).isNull();
    }

    @Test
    void updateChunk_whenNotOwnedByEmpresa_throwsNotFound() {
        RagChunk chunk = RagChunk.builder().id("chunk-1").documentId("doc-1").chunk(0).text("velho").build();
        when(ragChunkRepository.findById("chunk-1")).thenReturn(Optional.of(chunk));
        when(ragDocumentRepository.findByIdAndEmpresaId("doc-1", OTHER_EMPRESA_ID)).thenReturn(Optional.empty());

        RagChunkRequest request = new RagChunkRequest(null, "novo texto");

        assertThatThrownBy(() -> ragService.updateChunk("chunk-1", OTHER_EMPRESA_ID, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void search_whenEmpresaHasNoDocuments_returnsEmptyPageWithoutQueryingChunks() {
        when(ragDocumentRepository.findIdOnlyByEmpresaId(EMPRESA_ID)).thenReturn(List.of());
        Pageable pageable = PageRequest.of(0, 20);
        when(ragChunkRepository.searchByTextInDocuments("estoque", List.of(), pageable))
                .thenReturn(Page.empty(pageable));

        Page<RagChunkResponse> result = ragService.search(EMPRESA_ID, "estoque", pageable);

        assertThat(result.getContent()).isEmpty();
        verify(ragChunkRepository).searchByTextInDocuments("estoque", List.of(), pageable);
    }

    @Test
    void search_whenEmpresaHasDocuments_delegatesToRepositoryWithResolvedIds() {
        RagDocumentRepository.IdOnly doc1 = () -> "doc-1";
        RagDocumentRepository.IdOnly doc2 = () -> "doc-2";
        when(ragDocumentRepository.findIdOnlyByEmpresaId(EMPRESA_ID)).thenReturn(List.of(doc1, doc2));

        RagChunk chunk = RagChunk.builder().id("chunk-1").documentId("doc-1").chunk(0).text("estoque baixo").build();
        Pageable pageable = PageRequest.of(0, 20);
        when(ragChunkRepository.searchByTextInDocuments("estoque", List.of("doc-1", "doc-2"), pageable))
                .thenReturn(new PageImpl<>(List.of(chunk), pageable, 1));

        Page<RagChunkResponse> result = ragService.search(EMPRESA_ID, "estoque", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).text()).isEqualTo("estoque baixo");
    }
}
