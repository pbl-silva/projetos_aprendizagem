/**
 * Por padrao, parametros, tipos de retorno e campos neste pacote sao considerados
 * @NonNull, a menos que explicitamente anotados com @Nullable. Isso alinha o codigo
 * com as anotacoes de nulidade do proprio Spring (Spring Data, Spring MVC etc.) e
 * elimina os avisos de "unchecked conversion" do compilador sem suprimi-los.
 */
@org.springframework.lang.NonNullApi
@org.springframework.lang.NonNullFields
package com.ecommerce.api.security;