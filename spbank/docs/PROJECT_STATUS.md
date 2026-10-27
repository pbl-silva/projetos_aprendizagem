# Estado do SPBank

Atualizado em 27/10/2026. Escopo funcional do guia concluído.

| Módulo | Estado |
|---|---|
| Autenticação, contas, planos, gestão e transferências | Implementados; regressão aprovada |
| Pix | Backend preservado; interface de chaves, favoritos, prévia, envio, comprovante, agendamentos, recorrência e cancelamento integrada |
| Cartão | Instrumentos, crédito, faturas, pagamento, exportação e renovação concluídos |
| Investimentos INV-1 a INV-4 | Catálogo, aplicações, calendário, ordens pendentes e valorização auditável preservados |
| Investimentos INV-5 | Resgate parcial/total, cotização, crédito D+1 útil e vencimento automático do CDB concluídos |
| Pagamentos PAG-1 | Parser de boleto/convênio, gerador personalizado, prévia, senha, idempotência, débito e comprovante concluídos |
| Recargas PAG-2 | Doze produtos, celular validado, logos, confirmação, histórico e comprovante concluídos |
| Agendamentos PAG-3 | Criação sem reserva, execução, revalidação, falha de negócio, nova tentativa técnica e cancelamento concluídos |
| Inicialização | README, configuração de exemplo, Compose opcional e servidor estático Node incluídos |

Validação: **274 testes Java, zero falhas, zero erros e zero testes ignorados**; compilação e pacote aprovados. A análise JDT de produção e testes terminou sem diagnósticos de nulabilidade, com as três violações do controle negativo detectadas.

MySQL 8.0.46 real: V1–V5 aplicadas desde schema vazio, Hibernate inicializado e health UP. A atualização incremental também foi validada, sem recriar qualquer base de uso.

As verificações HTTP cobriram senha, titularidade, idempotência, concorrência, agendamentos, resgates, vencimento, saldo e lançamentos. Erros de gravação forçados comprovaram rollback e nova tentativa; um agendamento com falha técnica não impediu o próximo item. O Chrome confirmou pagamentos, recargas, agendamento/cancelamento, Pix e resgate, sem exceções de JavaScript e sem transbordamento horizontal em 390 px.

Detalhes e reprodução: [conclusao-projeto.md](conclusao-projeto.md). Inicialização: [README](../README.md). Tributação, suitability e integração com redes bancárias reais ficam fora do escopo do laboratório.
