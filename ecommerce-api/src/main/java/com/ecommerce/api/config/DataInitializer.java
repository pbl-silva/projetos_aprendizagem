package com.ecommerce.api.config;

import com.ecommerce.api.model.Categoria;
import com.ecommerce.api.model.Produto;
import com.ecommerce.api.repository.CategoriaRepository;
import com.ecommerce.api.repository.ProdutoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner carregarCatalogoInicial(CategoriaRepository categoriaRepository,
                                               ProdutoRepository produtoRepository) {
        return args -> {
            if (produtoRepository.count() > 0) {
                return;
            }

            Map<String, Categoria> categorias = new LinkedHashMap<>();
            categorias.put("Eletrônicos", obterOuCriar(categoriaRepository,
                "Eletrônicos", "Tecnologia, áudio e acessórios"));
            categorias.put("Casa e Cozinha", obterOuCriar(categoriaRepository,
                "Casa e Cozinha", "Utilidades para uma casa mais prática"));
            categorias.put("Livros", obterOuCriar(categoriaRepository,
                "Livros", "Conhecimento, literatura e desenvolvimento"));
            categorias.put("Esportes", obterOuCriar(categoriaRepository,
                "Esportes", "Equipamentos para movimento e bem-estar"));
            categorias.put("Beleza", obterOuCriar(categoriaRepository,
                "Beleza", "Cuidados pessoais e perfumaria"));
            categorias.put("Games", obterOuCriar(categoriaRepository,
                "Games", "Jogos e acessórios para gamers"));
            categorias.put("Moda", obterOuCriar(categoriaRepository,
                "Moda", "Peças versáteis para o dia a dia"));

            List<Produto> produtos = new ArrayList<>();
            adicionar(produtos, categorias.get("Eletrônicos"),
                item("Notebook Aurora 14", "Notebook leve com SSD de 512 GB", "4299.90", 12),
                item("Fone Wave Pro", "Fone sem fio com cancelamento de ruído", "499.90", 28),
                item("Teclado Mecânico Flow", "Teclado compacto com iluminação ajustável", "349.90", 18),
                item("Smartwatch Pulse", "Relógio inteligente com monitor cardíaco", "799.90", 16));
            adicionar(produtos, categorias.get("Casa e Cozinha"),
                item("Cafeteira Essenza", "Cafeteira programável de 1,2 litro", "289.90", 22),
                item("Air Fryer Compact", "Fritadeira sem óleo de 4 litros", "459.90", 15),
                item("Jogo de Panelas Terra", "Conjunto antiaderente com cinco peças", "629.90", 9),
                item("Luminária Nórdica", "Luminária de mesa com luz quente", "159.90", 30));
            adicionar(produtos, categorias.get("Livros"),
                item("Código Limpo", "Práticas para escrever software sustentável", "119.90", 25),
                item("Arquitetura de Software", "Fundamentos de sistemas modernos", "139.90", 17),
                item("O Pequeno Príncipe", "Edição ilustrada em capa dura", "49.90", 40),
                item("Hábitos Atômicos", "Estratégias práticas para bons hábitos", "69.90", 33));
            adicionar(produtos, categorias.get("Esportes"),
                item("Tapete Yoga Balance", "Tapete antiderrapante de alta densidade", "129.90", 26),
                item("Kit Halteres Move", "Par de halteres ajustáveis", "399.90", 11),
                item("Garrafa Térmica Trail", "Garrafa inox de 750 ml", "119.90", 35),
                item("Bola de Futebol Arena", "Bola costurada para campo e society", "149.90", 19));
            adicionar(produtos, categorias.get("Beleza"),
                item("Sérum Vitamina C", "Sérum facial antioxidante de 30 ml", "89.90", 32),
                item("Kit Spa em Casa", "Sabonete, hidratante e sais de banho", "179.90", 14),
                item("Secador Ionic Air", "Secador compacto de 2000 W", "329.90", 10));
            adicionar(produtos, categorias.get("Games"),
                item("Controle Horizon", "Controle sem fio multiplataforma", "379.90", 21),
                item("Headset Nebula", "Headset surround com microfone removível", "449.90", 13),
                item("Mouse Vector", "Mouse de alta precisão com seis botões", "219.90", 27));
            adicionar(produtos, categorias.get("Moda"),
                item("Mochila Urban", "Mochila impermeável com espaço para notebook", "249.90", 24),
                item("Tênis Casual Nuvem", "Tênis leve para uso diário", "299.90", 20),
                item("Jaqueta Corta-Vento", "Jaqueta compacta e resistente à água", "359.90", 12));

            produtoRepository.saveAll(produtos);
        };
    }

    private Categoria obterOuCriar(CategoriaRepository repository, String nome, String descricao) {
        return repository.findByNome(nome)
            .orElseGet(() -> repository.save(
                Categoria.builder().nome(nome).descricao(descricao).ativo(true).build()));
    }

    private Produto item(String nome, String descricao, String preco, int estoque) {
        return Produto.builder()
            .nome(nome)
            .descricao(descricao)
            .preco(new BigDecimal(preco))
            .estoque(estoque)
            .ativo(true)
            .build();
    }

    private void adicionar(List<Produto> destino, Categoria categoria, Produto... produtos) {
        for (Produto produto : produtos) {
            produto.setCategoria(categoria);
            destino.add(produto);
        }
    }
}
