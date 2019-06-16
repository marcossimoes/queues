(ns queues.models.agent
  (:require [clojure.spec.alpha :as s]
            [queues.models.job :as job]))

(s/def ::id uuid?)
(s/def ::name string?)
(s/def ::primary-skillset (s/coll-of ::job/type :into [] :count 1))
(s/def ::secondary-skillset (s/coll-of ::job/type :into [] :count 1))

(s/def ::agent (s/keys :req [::id ::primary-skillset ::secondary-skillset]))