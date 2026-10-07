package br.com.spbank.pix.adapter.out.persistence.mysql.repository;

import br.com.spbank.pix.adapter.out.persistence.mysql.data.PixFavoriteData;
import br.com.spbank.pix.application.model.PixKeyType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PixFavoriteJpaRepository
        extends JpaRepository<PixFavoriteData, UUID> {

    Optional<PixFavoriteData>
        findBySourceAccountIdAndKeyTypeAndNormalizedKey(
            UUID source,
            PixKeyType type,
            String normalizedKey
        );

    @Query("""
        select f
          from PixFavoriteData f
         where f.sourceAccountId = :source
           and f.keyType = :type
           and f.normalizedKey = :normalizedKey
           and (
                f.routing =
                    br.com.spbank.pix.application.model.PixRouting.EXTERNAL
             or exists (
                 select k.id
                   from PixKeyData k
                  where k.id = f.pixKeyId
                    and k.active = true
             )
           )
        """)
    Optional<PixFavoriteData> findUsable(
        @Param("source")
        UUID source,

        @Param("type")
        PixKeyType type,

        @Param("normalizedKey")
        String normalizedKey
    );

    Optional<PixFavoriteData>
        findByIdAndSourceAccountId(
            UUID id,
            UUID source
        );

    @Query("""
        select f
          from PixFavoriteData f
         where f.sourceAccountId = :source
           and (
                f.pixKeyId is null
             or exists (
                 select k.id
                   from PixKeyData k
                  where k.id = f.pixKeyId
                    and k.active = true
             )
           )
         order by f.createdAt desc
        """)
    List<PixFavoriteData> findActiveBySource(
        @Param("source")
        UUID source
    );

    void deleteByPixKeyId(
        UUID pixKeyId
    );
}