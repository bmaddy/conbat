# conbat

A multiplayer Conway's Game of Life battle.

## Usage

* install node
* $ lein cljsbuild once
* $ node main.js
* $ open http://localhost:1337

## Notes

* The performance totally sucks right now. The problem is in the step function. Perhaps I'll feel ambitious again someday and fix it. Patches welcome ;)
* I was having problems getting the browser repl working so I disabled it.

## License

Copyright © 2013 FIXME

Distributed under the Eclipse Public License, the same as Clojure.
