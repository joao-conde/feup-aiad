# Description

The main project was divided into 2 smaller projects.

The first one consisted in the design and implementation of the multi-agent system itself.

The second one consisted in the collection of data from the execution of the system. Then, using data mining techniques we were asked to perform relevant classifications and regressions.


In project 1, a multi-agent system simulating an English auction was built.

1. Multiple buyers had a list of items they desired. Simultaneously, multiple sellers announced the auction of several items.
2. Each seller fixed a minimal price for each item they sell and uses it as the initial value for the auction. Thus, the same item, may have different prices according to each seller.
3. The maximum value a buyer is willing to spend on an item is influenced by some parameters such as the auction price, the seller's reputation in the market and the estimated delivery time of the item after buying it.
4. The reputation of a seller is given by the opinions of the system' buyers that previously bought something from him. 
5. The reputation of a seller decreases if he fails to deliver items on the time estabilished upon purchase.


In project 2 we ran the system multiple times, extracting multiple parameters and accumulating aprox. 65 000 lines of data. 
We've then feed the data into our own RapidMiner processes to perform a classification on wether a seller sold an item or not and a regresion to determine at which price he could sell it.