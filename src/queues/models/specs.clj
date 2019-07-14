(ns queues.models.specs
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.test.alpha :as stest]))

;; General Specs

(s/def ::id string?)
(s/def ::skill string?)

;; Generating Support

(def agent-ids (gen/sample (gen/not-empty (s/gen ::id)) 5))
(def skills (gen/sample (gen/not-empty (s/gen ::skill)) 3))

;;Job Spec

(s/def ::job.id ::id)
(s/def ::job.type (s/with-gen ::skill #(gen/elements skills)))
(s/def ::job.urgent boolean?)
(s/def ::job (s/keys :req [::job.id ::job.type ::job.urgent]))

;; Agent Spec

(s/def ::agent.id ::id)
(s/def ::agent.name string?)
(s/def ::agent.primary-skillset (s/coll-of ::skill :into [] :count 1))
(s/def ::agent.secondary-skillset (s/coll-of ::skill :into [] :max-count 1))

(s/def ::agent-wo-cus-gen (s/keys :req [::agent.id
                                        ::agent.name
                                        ::agent.primary-skillset
                                        ::agent.secondary-skillset]))

(defn agent-overrider
  "Receives a sample of skills and agent id and
   returns a map that can serve as an overrider
   for a generated agent. This overrider will substitute
   the originally generated agent'id and skill"
  [skills id]
  {::id                 #(s/gen #{id})
   ::agent.primary-skillset   #(gen/vector (gen/elements skills) 1)
   ::agent.secondary-skillset #(gen/vector (gen/elements skills) 0 1)})

(defn agent-from-id-and-skills
  "Receives a agent generator,
  a sample of skills, and an id
  and returns random sample agent with that
  id and one of the provided skills"
  [generator skills id]
  (-> generator
      (s/gen (agent-overrider skills id))
      (gen/generate)))

(def agents (map (partial agent-from-id-and-skills ::agent-wo-cus-gen skills)
                 agent-ids))

(s/def ::agent (s/with-gen (s/keys :req [::agent.id
                                         ::agent.name
                                         ::agent.primary-skillset
                                         ::agent.secondary-skillset])
                           #(gen/elements agents)))

;; Job Request

(s/def ::job-req.agent-id (s/with-gen ::agent.id #(gen/elements agent-ids)))
(s/def ::job-req (s/keys :req [::job-req.agent-id]))

;; Job Assigned

(s/def ::job-assigned.job-id ::job.id)
(s/def ::job-assigned.agent-id ::agent.id)
(s/def ::job-assigned (s/keys :req [::job-assigned.job-id
                                    ::job-assigned.agent-id]))

;; Priority

(s/def ::priority.skill-type #{::agent.primary-skillset ::agent.secondary-skillset})
(s/def ::priority.urgent ::job.urgent)
(s/def ::priority (s/keys :req [::priority.skill-type
                                ::priority.urgent]))

;; Queues Spec

(s/def ::agents (s/coll-of ::agent
                           ;;:distinct true
                           :into []))

(s/def ::jobs-assigned (s/coll-of (s/keys :req [::job-assigned])
                                  ;;:distinct true
                                  :into []))

(s/def ::jobs-waiting (s/coll-of ::job
                                 ;;:distinct true
                                 :into []))

(s/def ::job-requests-waiting (s/coll-of ::job-req
                                         ;;:distinct true
                                         :into []))

(s/def ::priority-queue (s/coll-of ::priority
                                   :distinct true
                                   :into []
                                   :min-count 1
                                   :max-count 4))

(s/def ::agents-and-jobs (s/keys :req [::agents
                                       ::jobs-assigned
                                       ::jobs-waiting
                                       ::job-requests-waiting]))

;; Events Spec

(s/def ::new-agent ::agent)
(s/def ::new-job ::job)
(s/def ::job-request ::job-req)

(s/def ::new-agent-event (s/keys :req [::new-agent]))
(s/def ::new-job-event (s/keys :req [::new-job]))
(s/def ::job-request-event (s/keys :req [::job-request]))

(s/def ::event (s/or :new-agent-event ::new-agent-event
                     :new-job-event ::new-job-event
                     :job-request-event ::job-request-event))

(s/def ::events (s/coll-of ::event
                           :distinct true
                           :into []))

(stest/instrument)

