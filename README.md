# Client for NeverSleep

NeverSleep is an immutable data structure server, written in Clojure

## Server installation

Follow instructions at [https://github.com/raspasov/neversleep](https://github.com/raspasov/neversleep)

## Usage
Write to an entity "user-1" while preserving all past history:
```clojure
;first write
(io-assoc "user-1" :currency-balance 100)
=> {:result {:timestamp "1427531747415000000"}} ;t-1
;second write
(io-assoc "user-1" :currency-balance 200)
=> {:result {:timestamp "1427531776863000000"}} ;t-2
;read "user-1" as of t-1
(io-get-entity-as-of "user-1" "1427531747415000000")
=> {:result {:currency-balance 100}}
;read "user-1" as of t-2
(io-get-entity-as-of "user-1" "1427531776863000000")
{:result {:currency-balance 200}}
```


## License

Copyright © 2015 Rangel Spasov

Distributed under the MIT License
