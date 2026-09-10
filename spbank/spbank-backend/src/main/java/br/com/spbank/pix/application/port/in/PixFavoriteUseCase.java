package br.com.spbank.pix.application.port.in;

import br.com.spbank.pix.application.model.PixFavorite;

import java.util.List;
import java.util.UUID;

public interface PixFavoriteUseCase {

    List<PixFavorite> list(UUID sourceAccountId);

    PixFavorite register(PixFavoriteRegistrationCommand command);

    void remove(UUID favoriteId, UUID sourceAccountId);
}