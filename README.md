# feup-aiad

## description

No cenário em questão, um ou mais compradores possuem uma lista de produtos que pretendem comprar. Simultaneamente, vários vendedores anunciarão a venda de diversos produtos a preços iniciais diferentes entre estes.

Cada vendedor tem um valor mínimo para cada produto que vende e que está disposto a aceitar, sendo este o valor inicial do produto em leilão(English auction).

Cada comprador estará disposto a apostar um valor máximo por cada produto associado ao seu vendedor. Os compradores têm perfis diferentes e podem arriscar mais ou menos nesta aposta. Este valor máximo que se dispõe a dar será também influenciado por diferentes métricas. Assim, o preço, a reputação do vendedor e o tempo estimado de entrega influenciarão a decisão do comprador.

A reputação de um vendedor é dada pelas opiniões de outros compradores do sistema que já lhe compraram algum produto. Caso não exista nenhuma reputação prévia terá um valor inicial médio. A opinião de cada comprador do sistema sobre os vendedores aos quais já compraram algum produto é calculada com base no tempo prometido de entrega dos produtos comprados e do tempo realmente verificado.

Sempre que um vendedor entra no sistema informa o InformationSystemAgent que é responsável por manter registo de todos os vendedores existentes bem como quais os produtos que cada um deles está a vender.

Existe um agente BidKeeper responsável por gerir as apostas já terminadas e ainda não pagas. Este agente é importante para que se assegure que uma aposta já terminada seja efetivamente completada uma vez que um comprador pode apostar em vários leilões ao mesmo tempo e consequentemente, caso ganhe mais do que 1 aposta do mesmo produto, abdicar de uma delas. O BidKeeper tem um tempo máximo para manter as apostas ganhas mas ainda não pagas. Após esse periodo, a aposta é considerada não liquidada e o vendedor poderá iniciar novamente a venda desse produto, reiniciando o leilão.

O cliente mantém-se no sistema até conseguir comprar todos os produtos que pretende ou até atingir o tempo limite determinado ou ficar sem orçamento.




-- old version

O tema deste projeto prende-se com a compra e venda de bens num mercado online aberto a negociação. Assim existem 2 tipos de agentes: vendedores e clientes. Os primeiros tentam vender os seus produtos ao maior preço possível, pretendendo contudo aumentar o seu valor de satisfação média. Esta satisfação é atribuída por cada cliente que compre um produto e tem em conta o preço pelo qual o cliente comprou bem como o tempo que este produto demorou a chegar. Aquando da compra de cada produto, um cliente estabelece quais as suas prioridades, nomeadamente relativamente ao preço máximo, bem como qual a importância dos fatores “preço”, “satisfação do vendedor” e “tempo de entrega” estabelecendo assim um valor final para cada uma das ofertas. Após as várias inquisições e negociações o cliente escolherá a oferta que terá o melhor valor.

#### Dependent variables
	- valor de compra (valor a que um item foi comprado relativamente ao preço base do vendedor)
	- items por comprar (preço mínimo superior ao que o cliente tinha disponível)
	- satisfação

#### Independent variables
	- range do preço base de cada item
	- preço mínimo que está disposto a vender (vendedor)
	- tempo de entrega
