(ns queues.specs.agents
  (:require [clojure.spec.alpha :as s]
            [queues.specs.general :as specs.general]))

(s/def ::id ::specs.general/id)
(s/def ::name string?)

(s/def ::skillset (s/coll-of ::specs.general/skill :into []))
(s/def ::primary-skillset (s/coll-of ::specs.general/skill :into []))
(s/def ::secondary-skillset (s/coll-of ::specs.general/skill :into []))

(s/def ::skillset-type keyword?)

;;TODO [IMPROVE; ARCH] smells wrong
(def skillset-types-in-order-of-priority [::primary-skillset ::secondary-skillset])

(s/def ::agent (s/keys :req [::id
                             ::name
                             ::primary-skillset
                             ::secondary-skillset]))

;;TODO [READ] merge agent with agents

(s/def ::all-agents (s/map-of ::id ::agent))