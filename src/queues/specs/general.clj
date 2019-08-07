(ns queues.specs.general
  (:require [clojure.spec.alpha :as s]))

(s/def ::id string?)
(s/def ::skill string?)