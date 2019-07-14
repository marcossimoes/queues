(ns queues.models.agent
  (:require [clojure.spec.alpha :as s]
            [queues.models.job :as job]))

(s/def ::id string?)
(s/def ::name string?)
(s/def ::skillset ::job/type)
(s/def ::primary-skillset (s/coll-of ::skillset :into [] :count 1))
(s/def ::secondary-skillset (s/coll-of ::skillset :into [] :max-count 1))

(s/def ::agent (s/keys :req [::id ::name ::primary-skillset ::secondary-skillset]))