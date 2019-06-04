(ns queues.models.agent
  (:require [clojure.spec.alpha :as s]
            [queues.models.job :as job]))

(s/def ::id uuid?)
(s/def ::primary-skills (s/coll-of ::job/type :distinct true :into #{}))
(s/def ::secondary-skills (s/coll-of ::job/type :distinct true :into #{}))

(s/def ::agent (s/keys :req [::id ::primary-skills ::secondary-skills]))