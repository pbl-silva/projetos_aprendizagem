package br.com.spbank.pix.application.port.in;

import br.com.spbank.pix.application.model.PixFavorite;

import java.util.List;
import java.util.UUID;

public interface PixFavoriteUseCase {

    PixFavorite registerFavorite(
        PixFavoriteRegistrationCommand command
    );

    List<PixFavorite> listFavorites(
        UUID accountId
    );

    void deleteFavorite(
        UUID accountId,
        UUID favoriteId
    );
}