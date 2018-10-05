
# Description

No cenário em questão, um ou mais compradores possuem uma lista de produtos que pretendem comprar. Simultaneamente, vários vendedores anunciarão a venda de diversos produtos.

Cada vendedor fixa um preço mínimo para cada produto que vende, sendo este o valor inicial do produto em leilão (English auction). O mesmo produto, oferecido por diferentes vendedores, pode ter preços diferentes.

Cada comprador estará disposto a gastar um valor máximo por cada produto associado ao respetivo vendedor. Os compradores têm perfis diferentes e podem arriscar mais ou menos nestes leilões, definindo um valor máximo maior ou menor. O valor máximo que cada comprador se dispõe a gastar será também influenciado por diferentes métricas. Assim, o preço inicial do leilão, a reputação do vendedor e o tempo estimado de entrega do produto influenciarão a decisão do comprador.

A reputação de um vendedor é dada pelas opiniões dos compradores do sistema que já lhe compraram algum produto. Caso não exista nenhuma transação prévia para o vendedor em questão, a sua reputação terá um valor inicial médio. A opinião de cada comprador do sistema sobre os vendedores aos quais já comprou algum produto é calculada com base no tempo prometido de entrega dos produtos comprados e no tempo realmente verificado.

Sempre que um vendedor entra no sistema informa o InformationSystemAgent, que é responsável por manter registo de todos os vendedores existentes, bem como dos produtos que cada um deles está a vender.

Existe um agente BidKeeper responsável por gerir os leilões já terminados e ainda não pagos. Este agente é importante para que se assegure que um leilão já terminado seja efetivamente completado. De facto, um comprador pode licitar em vários leilões ao mesmo tempo e, caso ganhe mais do que um leilão do mesmo produto, pode abdicar de um deles. O BidKeeper tem um tempo máximo para manter os leilões terminados mas ainda não pagos. Após esse período, o leilão é considerado não liquidado e o vendedor poderá iniciar novamente a venda do produto, reiniciando o leilão.

O cliente mantém-se no sistema até conseguir comprar todos os produtos que pretende ou até atingir o tempo limite determinado ou ficar sem orçamento.
