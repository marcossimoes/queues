(ns queues.models.job
  (:require [clojure.spec.alpha :as s]))

(s/def ::id string?)
(s/def ::type string?)
(s/def ::urgent boolean?)

(s/def ::job (s/keys :req [::id ::type ::urgent]))
