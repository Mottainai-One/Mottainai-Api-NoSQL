package com.mottainai.mongoapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
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
import com.mottainai.mongoapi.exception.DuplicateDocumentException;
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
                .title("Título").source("manual_operacional").category("MANUAL").version("1.0").build();
    }

    private RagDocumentRequest sampleRequest(String slug, Long empresaId, String title, String source, String version) {
        return new RagDocumentRequest(slug, empresaId, title, source, "MANUAL", version, null);
    }

    @Test
    void createDocument_whenTitleAndVersionUnique_savesDocument() {
        RagDocumentRequest request = sampleRequest("novo-slug", EMPRESA_ID, "Título", "manual_operacional", "1.0");
        when(ragDocumentRepository.findFirstBySlugAndEmpresaIdOrderByCreatedAtDesc("novo-slug", EMPRESA_ID))
                .thenReturn(Optional.empty());
        when(ragDocumentRepository.findByEmpresaIdAndTitleAndVersion(EMPRESA_ID, "Título", "1.0"))
                .thenReturn(Optional.empty());
        when(ragDocumentRepository.save(any(RagDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RagDocumentResponse response = ragService.createDocument(request);

        assertThat(response.slug()).isEqualTo("novo-slug");
        assertThat(response.empresaId()).isEqualTo(EMPRESA_ID);
        assertThat(response.version()).isEqualTo("1.0");
    }

    @Test
    void createDocument_withoutSlug_savesDocumentAndAppliesDefaultVersion() {
        // Documento legado sem slug; o Mongo aceita um por empresa.
        RagDocumentRequest request = new RagDocumentRequest(null, EMPRESA_ID, "FAQ Promoções", null, "FAQ", null, null);
        when(ragDocumentRepository.findByEmpresaIdAndTitleAndVersion(EMPRESA_ID, "FAQ Promoções", "1.0"))
                .thenReturn(Optional.empty());
        when(ragDocumentRepository.save(any(RagDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<RagDocument> captor = ArgumentCaptor.forClass(RagDocument.class);
        RagDocumentResponse response = ragService.createDocument(request);

        verify(ragDocumentRepository).save(captor.capture());
        assertThat(captor.getValue().getVersion()).isEqualTo("1.0");
        assertThat(response.slug()).isNull();
        assertThat(response.category()).isEqualTo("FAQ");
        verify(ragDocumentRepository).findFirstBySlugAndEmpresaIdOrderByCreatedAtDesc(null, EMPRESA_ID);
    }

    @Test
    void createDocument_whenTitleAndVersionDuplicate_throwsDuplicateDocumentException() {
        RagDocumentRequest request = sampleRequest("existente", EMPRESA_ID, "Título", "manual_operacional", "1.0");
        when(ragDocumentRepository.findFirstBySlugAndEmpresaIdOrderByCreatedAtDesc("existente", EMPRESA_ID))
                .thenReturn(Optional.empty());
        when(ragDocumentRepository.findByEmpresaIdAndTitleAndVersion(EMPRESA_ID, "Título", "1.0"))
                .thenReturn(Optional.of(sampleDocument("id-1", EMPRESA_ID, "existente")));

        assertThatThrownBy(() -> ragService.createDocument(request))
                .isInstanceOf(DuplicateDocumentException.class);

        verify(ragDocumentRepository, never()).save(any());
    }

    @Test
    void createDocument_whenSlugDuplicate_throwsDuplicateDocumentException() {
        RagDocumentRequest request = sampleRequest("existente", EMPRESA_ID, "Título", "manual_operacional", "1.0");
        when(ragDocumentRepository.findFirstBySlugAndEmpresaIdOrderByCreatedAtDesc("existente", EMPRESA_ID))
                .thenReturn(Optional.of(sampleDocument("id-1", EMPRESA_ID, "existente")));

        assertThatThrownBy(() -> ragService.createDocument(request))
                .isInstanceOf(DuplicateDocumentException.class);

        verify(ragDocumentRepository, never()).save(any());
    }

    @Test
    void createDocument_sameSlugFromAnotherEmpresa_isAllowed() {
        RagDocumentRequest request = sampleRequest("slug-compartilhado", OTHER_EMPRESA_ID,
                "Outro título", "faq", "1.0");
        when(ragDocumentRepository.save(any(RagDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RagDocumentResponse response = ragService.createDocument(request);

        verify(ragDocumentRepository)
                .findFirstBySlugAndEmpresaIdOrderByCreatedAtDesc("slug-compartilhado", OTHER_EMPRESA_ID);
        assertThat(response.empresaId()).isEqualTo(OTHER_EMPRESA_ID);
    }

    @Test
    void createDocument_withoutCategory_derivesItFromSource() {
        RagDocumentRequest request = new RagDocumentRequest("faq-novo", EMPRESA_ID,
                "Perguntas frequentes", "faq_publica", null, null, null);
        when(ragDocumentRepository.save(any(RagDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RagDocumentResponse response = ragService.createDocument(request);

        assertThat(response.category()).isEqualTo("FAQ");
        assertThat(response.version()).isEqualTo("1.0");
    }

    @Test
    void createDocument_secondMissingSlugForEmpresa_throwsConflict() {
        RagDocumentRequest request = new RagDocumentRequest(null, EMPRESA_ID,
                "Outro documento", null, "MANUAL", null, null);
        when(ragDocumentRepository.findFirstBySlugAndEmpresaIdOrderByCreatedAtDesc(null, EMPRESA_ID))
                .thenReturn(Optional.of(sampleDocument("id-1", EMPRESA_ID, null)));

        assertThatThrownBy(() -> ragService.createDocument(request))
                .isInstanceOf(DuplicateDocumentException.class);
        verify(ragDocumentRepository, never()).save(any());
    }

    @Test
    void getDocument_whenBelongsToAnotherEmpresa_throwsNotFound() {
        when(ragDocumentRepository.findByIdAndEmpresaId("id-1", OTHER_EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ragService.getDocument("id-1", OTHER_EMPRESA_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getDocumentBySlug_returnsDocumentForEmpresa() {
        RagDocument document = sampleDocument("id-2", EMPRESA_ID, "slug-1");
        when(ragDocumentRepository.findFirstBySlugAndEmpresaIdOrderByCreatedAtDesc("slug-1", EMPRESA_ID))
                .thenReturn(Optional.of(document));

        RagDocumentResponse response = ragService.getDocumentBySlug("slug-1", EMPRESA_ID);

        assertThat(response.id()).isEqualTo("id-2");
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
    void updateDocument_whenNewSlugUsedByAnotherDocument_throwsDuplicateDocumentException() {
        RagDocument document = sampleDocument("id-1", EMPRESA_ID, "slug-atual");
        RagDocument other = sampleDocument("id-2", EMPRESA_ID, "slug-alvo");
        RagDocumentRequest request = sampleRequest("slug-alvo", EMPRESA_ID, "Novo título", "faq", "2.0");

        when(ragDocumentRepository.findByIdAndEmpresaId("id-1", EMPRESA_ID)).thenReturn(Optional.of(document));
        when(ragDocumentRepository.findFirstBySlugAndEmpresaIdOrderByCreatedAtDesc("slug-alvo", EMPRESA_ID))
                .thenReturn(Optional.of(other));

        assertThatThrownBy(() -> ragService.updateDocument("id-1", EMPRESA_ID, request))
                .isInstanceOf(DuplicateDocumentException.class);
    }

    @Test
    void updateDocument_whenNewTitleVersionUsedByAnotherDocument_throwsDuplicateDocumentException() {
        RagDocument document = sampleDocument("id-1", EMPRESA_ID, "slug-atual");
        RagDocument other = sampleDocument("id-2", EMPRESA_ID, "slug-alvo");
        RagDocumentRequest request = sampleRequest(null, EMPRESA_ID, "Novo título", "faq", "2.0");

        when(ragDocumentRepository.findByIdAndEmpresaId("id-1", EMPRESA_ID)).thenReturn(Optional.of(document));
        when(ragDocumentRepository.findByEmpresaIdAndTitleAndVersion(EMPRESA_ID, "Novo título", "2.0"))
                .thenReturn(Optional.of(other));

        assertThatThrownBy(() -> ragService.updateDocument("id-1", EMPRESA_ID, request))
                .isInstanceOf(DuplicateDocumentException.class);
    }

    @Test
    void updateDocument_whenSlugAndTitleVersionBelongToItself_updatesNormally() {
        RagDocument document = sampleDocument("id-1", EMPRESA_ID, "slug-atual");
        RagDocumentRequest request = sampleRequest("slug-atual", EMPRESA_ID, "Título", "faq", "1.0");

        when(ragDocumentRepository.findByIdAndEmpresaId("id-1", EMPRESA_ID)).thenReturn(Optional.of(document));
        when(ragDocumentRepository.findFirstBySlugAndEmpresaIdOrderByCreatedAtDesc("slug-atual", EMPRESA_ID))
                .thenReturn(Optional.of(document));
        when(ragDocumentRepository.findByEmpresaIdAndTitleAndVersion(EMPRESA_ID, "Título", "1.0"))
                .thenReturn(Optional.of(document));
        when(ragDocumentRepository.save(any(RagDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RagDocumentResponse response = ragService.updateDocument("id-1", EMPRESA_ID, request);

        assertThat(response.source()).isEqualTo("faq");
    }

    @Test
    void createChunk_whenChunkNumberNotProvided_usesMaxPlusOne() {
        RagDocument document = sampleDocument("doc-1", EMPRESA_ID, "slug-1");
        RagChunkRequest request = new RagChunkRequest(null, "texto do novo chunk", null);

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
        RagChunkRequest request = new RagChunkRequest(null, "primeiro chunk", null);

        when(ragDocumentRepository.findByIdAndEmpresaId("doc-1", EMPRESA_ID)).thenReturn(Optional.of(document));
        when(ragChunkRepository.findTopByDocumentIdOrderByChunkDesc("doc-1")).thenReturn(Optional.empty());
        when(ragChunkRepository.save(any(RagChunk.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RagChunkResponse response = ragService.createChunk("doc-1", EMPRESA_ID, request);

        assertThat(response.chunk()).isEqualTo(0);
    }

    @Test
    void createChunk_whenChunkNumberProvided_usesProvidedValueAndNeverPersistsEmbedding() {
        RagDocument document = sampleDocument("doc-1", EMPRESA_ID, "slug-1");
        RagChunkRequest request = new RagChunkRequest(10, "texto explícito", Map.of("section", "manual"));

        when(ragDocumentRepository.findByIdAndEmpresaId("doc-1", EMPRESA_ID)).thenReturn(Optional.of(document));
        ArgumentCaptor<RagChunk> captor = ArgumentCaptor.forClass(RagChunk.class);
        when(ragChunkRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        ragService.createChunk("doc-1", EMPRESA_ID, request);

        verify(ragChunkRepository, never()).findTopByDocumentIdOrderByChunkDesc(any());
        RagChunk saved = captor.getValue();
        assertThat(saved.getChunk()).isEqualTo(10);
        assertThat(saved.getEmbedding()).isNull();
        assertThat(saved.getMetadata()).containsEntry("section", "manual");
    }

    @Test
    void updateChunk_whenTextChanges_invalidatesEmbedding() {
        RagChunk chunk = RagChunk.builder().id("chunk-1").documentId("doc-1")
                .chunk(0).text("texto antigo").embedding(List.of(0.1, 0.2)).build();
        when(ragChunkRepository.findById("chunk-1")).thenReturn(Optional.of(chunk));
        when(ragDocumentRepository.findByIdAndEmpresaId("doc-1", EMPRESA_ID))
                .thenReturn(Optional.of(sampleDocument("doc-1", EMPRESA_ID, "slug-1")));
        when(ragChunkRepository.save(any(RagChunk.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ragService.updateChunk("chunk-1", EMPRESA_ID, new RagChunkRequest(null, "novo texto", null));

        assertThat(chunk.getText()).isEqualTo("novo texto");
        assertThat(chunk.getEmbedding()).isNull();
        verify(ragChunkRepository).save(chunk);
    }

    @Test
    void updateChunk_whenOnlyMetadataOrIndexChanges_preservesEmbedding() {
        RagChunk chunk = RagChunk.builder().id("chunk-1").documentId("doc-1")
                .chunk(0).text("mesmo texto").embedding(List.of(0.1, 0.2)).build();
        when(ragChunkRepository.findById("chunk-1")).thenReturn(Optional.of(chunk));
        when(ragDocumentRepository.findByIdAndEmpresaId("doc-1", EMPRESA_ID))
                .thenReturn(Optional.of(sampleDocument("doc-1", EMPRESA_ID, "slug-1")));
        when(ragChunkRepository.save(any(RagChunk.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ragService.updateChunk("chunk-1", EMPRESA_ID,
                new RagChunkRequest(2, "mesmo texto", Map.of("section", "FAQ")));

        assertThat(chunk.getEmbedding()).containsExactly(0.1, 0.2);
        assertThat(chunk.getChunk()).isEqualTo(2);
        assertThat(chunk.getMetadata()).containsEntry("section", "FAQ");
    }

    @Test
    void updateChunk_whenNotOwnedByEmpresa_throwsNotFound() {
        RagChunk chunk = RagChunk.builder().id("chunk-1").documentId("doc-1").chunk(0).text("velho").build();
        when(ragChunkRepository.findById("chunk-1")).thenReturn(Optional.of(chunk));
        when(ragDocumentRepository.findByIdAndEmpresaId("doc-1", OTHER_EMPRESA_ID)).thenReturn(Optional.empty());

        RagChunkRequest request = new RagChunkRequest(null, "novo texto", null);

        assertThatThrownBy(() -> ragService.updateChunk("chunk-1", OTHER_EMPRESA_ID, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void search_delegatesToRepositoryRestrictedByEmpresaId() {
        RagDocument doc = sampleDocument("doc-1", EMPRESA_ID, "slug-1");
        Pageable pageable = PageRequest.of(0, 20);
        when(ragDocumentRepository.searchByTitleText(EMPRESA_ID, "estoque", pageable))
                .thenReturn(new PageImpl<>(List.of(doc), pageable, 1));

        Page<RagDocumentResponse> result = ragService.search(EMPRESA_ID, "estoque", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).slug()).isEqualTo("slug-1");
    }

    @Test
    void search_whenNoDocumentsMatch_returnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 20);
        when(ragDocumentRepository.searchByTitleText(EMPRESA_ID, "inexistente", pageable))
                .thenReturn(Page.empty(pageable));

        Page<RagDocumentResponse> result = ragService.search(EMPRESA_ID, "inexistente", pageable);

        assertThat(result.getContent()).isEmpty();
    }
}
