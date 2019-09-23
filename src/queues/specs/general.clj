(ns queues.specs.general
  (:require [clojure.spec.alpha :as s]))

(s/def ::id (s/and string? #(not= "" %)))
(s/def ::skill string?)