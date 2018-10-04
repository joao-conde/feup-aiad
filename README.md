# feup-aiad

## description

Vários vendedores para o mesmo produto com preços inicias diferentes .
Um comprador tem uma lista de produtos que pretende comprar.
Cada produto-vendedor tem um valor mínimo que está disposto a aceitar que é o valor inicial do leilão.
Um comprador por cada produto-vendedor tem um valor máximo que está disposto a apostar (sendo que os compradores têm perfis diferentes e podem arriscar mais ou menos). Este valor máximo que é medido pelas suas prioridades(preço, reputação do vendedor e tempo de entrega - distancia entre vendedor e o comprador)
A reputação de um vendedor é dada pelas opiniões de outros compradores do sistema que já lhe compraram algum produto. Caso não exista nenhuma reputação tem um valor médio. A opinião de cada comprador do sistema sobre os vendedores aos quais já comprou algum produto prende-se com o tempo prometido de entrega e do realmente verificado.
Sempre que um comprador entra no sistema informa o InformationSystemAgent que é responsável por manter registo de todos os vendedores existentes no sistema bem como quais os todos que cada um deles está a vender.
Existe um agente BidKeeper responsável por gerir as apostas já terminadas e ainda não pagas. Este agente é importante para que se assegure que uma aposta já terminada é efetivamente completada uma vez que um comprador pode apostar em vários leilões ao mesmo tempo e consequentemente, caso ganhe mais do que 1 aposta do mesmo produto abdicar de uma delas. O BidKeeper tem um tempo máximo para manter as apostas ainda não completas. Após esse tempo a aposta é considerada não terminada e o vendedor poderá iniciar novamente a venda dessa.
O cliente mantém-se no sistema até conseguir comprar todos os produtos que pretende ou até atingir o tempo limite determinado.

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
