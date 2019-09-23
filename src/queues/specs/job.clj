(ns queues.specs.job
  (:require [clojure.spec.alpha :as s]
            [queues.specs.general :as specs.general]))

(s/def ::id ::specs.general/id)
(s/def ::type ::specs.general/skill)
(s/def ::urgent boolean?)
(s/def ::assigned-agent ::id)
(s/def ::job (s/keys :req [::id ::type ::urgent]
                     :opt [::assigned-agent]))