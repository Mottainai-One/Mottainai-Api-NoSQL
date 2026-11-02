package com.mottainai.mongoapi.security;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class TenantAuthorization {
    private final OperationalMembership membership;
    public TenantAuthorization(OperationalMembership membership) { this.membership = membership; }
    public void authorize(Jwt jwt, Long requestedCompany) {
        try {
            java.util.UUID session = java.util.UUID.fromString(jwt.getClaimAsString("sid"));
            Number user = jwt.getClaim("usuario_id");
            Number company = jwt.getClaim("empresa_id");
            if (jwt.getSubject() == null || jwt.getSubject().isBlank() || user == null || company == null
                    || user.longValue() < 1 || company.longValue() < 1 || requestedCompany == null
                    || company.longValue() != requestedCompany || !"DONO".equals(jwt.getClaimAsString("role"))
                    || !"access".equals(jwt.getClaimAsString("use"))) throw new AccessDeniedException("Access denied");
            if (!membership.isActiveOwner(jwt.getSubject(), user.longValue(), company.longValue(), session))
                throw new AccessDeniedException("Access denied");
        } catch (ClassCastException | NullPointerException | IllegalArgumentException e) { throw new AccessDeniedException("Access denied"); }
    }
}
