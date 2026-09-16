package br.com.spbank.pix.adapter.out.persistence.mysql.repository;

import br.com.spbank.pix.adapter.out.persistence.mysql.data.PixFavoriteData;
import br.com.spbank.pix.application.model.PixDestinationScope;
import br.com.spbank.pix.application.model.PixKeyType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PixFavoriteJpaRepository
        extends JpaRepository<PixFavoriteData, UUID> {

    List<PixFavoriteData>
            findByOriginAccountIdOrderByCreatedAtAsc(
                UUID originAccountId
            );

    Optional<PixFavoriteData>
            findByIdAndOriginAccountId(
                UUID id,
                UUID originAccountId
            );

    Optional<PixFavoriteData>
            findByOriginAccountIdAndKeyTypeAndNormalizedKeyValue(
                UUID originAccountId,
                PixKeyType keyType,
                String normalizedKeyValue
            );

    @Query("""
        SELECT f
        FROM PixFavoriteData f
        WHERE f.originAccountId = :originAccountId
          AND f.keyType = :keyType
          AND f.normalizedKeyValue = :normalizedKeyValue
          AND (
                f.destinationScope = :externalScope
                OR (
                    f.destinationScope = :internalScope
                    AND EXISTS (
                        SELECT k.id
                        FROM PixKeyData k
                        WHERE k.id = f.pixKeyId
                          AND k.active = true
                    )
                )
          )
        """)
    Optional<PixFavoriteData> findUsableFavorite(
        @Param("originAccountId")
        UUID originAccountId,

        @Param("keyType")
        PixKeyType keyType,

        @Param("normalizedKeyValue")
        String normalizedKeyValue,

        @Param("internalScope")
        PixDestinationScope internalScope,

        @Param("externalScope")
        PixDestinationScope externalScope
    );

    void deleteByIdAndOriginAccountId(
        UUID id,
        UUID originAccountId
    );

    void deleteByPixKeyId(
        UUID pixKeyId
    );
}