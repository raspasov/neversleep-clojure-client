# Client for NeverSleep

NeverSleep is an immutable data structure server, written in Clojure

## Server installation

Follow instructions at [https://github.com/raspasov/neversleep](https://github.com/raspasov/neversleep)

## Usage
Write to an entity "user-1" while preserving all past history:
```clojure
;connect to server
(init "localhost" 10000)

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

;get all versions of an entity 
;the last parameter is the maximum number of versions returned (limit)
;this lookup is currently implemented in linear time, so the limit should be kept low
(io-get-all-versions-between "user-1" latest-server-timestamp end-of-times 25)
=> {:result [["1427531776863000000" {:currency-balance 200}] 
             ["1427531747415000000" {:currency-balance 100}]]}

;"delete" from an entity
(io-dissoc "user-1" :currency-balance)
=> {:result {:timestamp "1427533314969000000"}}

;get the current state of the entity
(io-get-entity "user-1")
=> {:result {}} ;entity is empty

;of course, reading "user-1" at a previous timestamp is intact
(io-get-entity-as-of "user-1" "1427531776863000000")
=> {:result {:currency-balance 200}}

;we can see that the history has grown by one item
=> {:result [["1427533314969000000" {}] 
            ["1427531776863000000" {:currency-balance 200}] 
            ["1427531747415000000" {:currency-balance 100}]]}

```


## License

Copyright © 2015 Rangel Spasov

Distributed under the MIT License
