# Estado do SPBank

Atualizado em 23/10/2026, com resultados locais confirmados.

| Módulo | Estado |
|---|---|
| Autenticação, contas, transferências e PIX | Implementação existente preservada; suíte de regressão aprovada |
| Cartão | Etapas concluídas nesta sequência, incluindo fluxo financeiro e interface; ver `cartao-concluido.md` |
| Investimentos INV-1/INV-2 | Catálogo tipado, prévia, confirmação segura, extrato, lotes e interface concluídos |
| Investimentos INV-3 | Calendário persistido e execução de aplicações pendentes implementados, com testes determinísticos |
| Investimentos INV-4 | Rentabilidade bruta por cota/CDI interno, avaliações auditáveis e interface concluídas |
| Investimentos INV-5 | Próximo checkpoint: resgates e vencimento automático |
| Pagamentos e recargas | Ainda não iniciados nesta sequência |

Validação mais recente: **234 testes Java aprovados**, análise de nulabilidade sem novos diagnósticos configurados, MySQL real com V1/V2 e Hibernate, 33 chamadas HTTP de INV-4, avaliações persistidas e fluxo de valorização/indisponibilidade no Chrome. Detalhes, limites e reprodução em [investimentos-rentabilidade.md](investimentos-rentabilidade.md). A validação anterior de aplicação, concorrência e rollback está em [investimentos-iniciais.md](investimentos-iniciais.md).

Desenvolvimento interrompido a pedido do usuário após INV-4. INV-5 não foi iniciado. A última correção declara explicitamente o retorno não nulo de InvestmentService.complete(), validado pelos 234 testes e pela análise JDT. Nenhuma base de uso foi recriada para validar esta etapa.
