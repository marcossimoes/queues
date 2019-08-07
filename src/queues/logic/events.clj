(ns queues.logic.events
  (:require [clojure.spec.alpha :as s]
            [clojure.tools.logging :as log]
            [queues.init :as init]
            [queues.logic.job-reqs :as job-reqs]
            [queues.logic.jobs :as jobs]
            [queues.logic.jobs-assigned :as jobs-assigned]
            [queues.specs.agent :as specs.agent]
            [queues.specs.events :as specs.events]
            [queues.specs.job :as specs.job]
            [queues.specs.job-request :as specs.job-request]
            [queues.specs.job-queues :as specs.job-queues]
            [clojure.pprint :as pp]))

(defn processed-new-agent
  "Adds a new-agent to the agents queue"
  [job-queues agent]
  (-> job-queues
      ::specs.job-queues/agents
      (send conj agent))
  agent)

(defn processed-new-job
  "Receives an 'agents and jobs' map and an event content and returns
  the 'agents and jobs' either with the new job assigned, if there were
  matching waiting job requests or queed in jobs waiting otherwise"
  [job-queues job-content]
  (dosync
    (let [matching-job-req (job-reqs/matching-waiting-job-request job-queues job-content)]
      (if (nil? matching-job-req)
        (jobs/queued-job job-queues job-content)
        (jobs-assigned/assigned-job job-queues matching-job-req job-content)))))

;;(s/fdef processed-new-job
;;        :args (s/cat :job-queues ::specs.job-queues/job-queues
;;                     :job-content ::specs.job/job)
;;        :ret ::specs.job-queues/job-queues
;;        :fn (s/or :queued-job #(= 1 (- (-> % :ret ::specs.job-queues/jobs-waiting)
;;                                       (-> % :args :job-queues ::specs.job-queues/jobs-waiting)))
;;                  :assigned-job #(= 1 (- (-> % :ret ::specs.job-queues/jobs-assigned)
;;                                         (-> % :args :job-queues ::specs.job-queues/jobs-assigned)))))

(defn dequeue
  "Given job-queues and a job-request-content,
  assigns the job-request to a waiting job and returns a map with {:job-id job-id}.
  If no waiting job is available, queues job-request in job-requests waiting
  and returns nil"
  [job-queues job-req-content]
  (dosync
    (let [matching-job (jobs/matching-waiting-job job-queues job-req-content)]
      (if (nil? matching-job)
        (job-reqs/queued-job-request job-queues job-req-content)
        (jobs-assigned/assigned-job job-queues job-req-content matching-job)))))

;;(s/fdef dequeue
;;        :args (s/cat :job-queues ::specs.job-queues/job-queues
;;                     :job-req-content ::specs.job-request/job-req)
;;        :ret ::specs.job-queues/job-queues
;;        :fn (s/or :queued-job-request#(= 1 (- (-> % :ret ::specs.job-queues/job-requests-waiting)
;;                                           (-> % :args :job-queues ::specs.job-queues/job-requests-waiting)))
;;                  :assigned-job #(= 1 (- (-> % :ret ::specs.job-queues/jobs-assigned)
;;                                         (-> % :args :job-queues ::specs.job-queues/jobs-assigned)))))

(defn added-event
  "Processes a new event and inserts the result in agents and jobs map"
  [job-queues event]
  (let [[type content] (first event)]
    (case type
      ::specs.events/new-agent (processed-new-agent job-queues content)
      ::specs.events/new-job (processed-new-job job-queues content)
      ::specs.events/job-request (dequeue job-queues content))))

;;(s/fdef added-event
;;        :args (s/cat :job-queues ::specs.job-queues/job-queues
;;                     :event ::specs.events/event)
;;        :ret ::specs.job-queues/job-queues
;;        :fn (s/or :new-agent #(= 1 (- (-> % :ret ::specs.job-queues/agents)
;;                                      (-> % :args :job-queues ::specs.job-queues/agents)))
;;                  :queued-job #(= 1 (- (-> % :ret ::specs.job-queues/jobs-waiting)
;;                                       (-> % :args :job-queues ::specs.job-queues/jobs-waiting)))
;;                  :queued-job-request#(= 1 (- (-> % :ret ::specs.job-queues/job-reqs-waiting)
;;                                           (-> % :args :job-queues ::specs.job-queues/job-reqs-waiting)))
;;                  :assigned-job #(= 1 (- (-> % :ret ::specs.job-queues/jobs-assigned)
;;                                         (-> % :args :job-queues ::specs.job-queues/jobs-assigned)))))

(defn added-event-with-log
  [job-queues event]
  ;;(println "event: " event)
  (let [res (added-event job-queues event)]
    (if init/*logging*
      (do (log/info "adding event: " event)
          (log/spyf :info "resulting event: %s" res)
          (print "resulting jqs: ")
          (pp/pprint init/job-queues)
          (print "\n"))
      res)))

;;(s/fdef added-event-with-log
;;        :args (s/cat :job-queues ::specs.job-queues/job-queues
;;                     :event ::specs.events/event)
;;        :ret ::specs.job-queues/job-queues
;;        :fn (s/or :new-agent #(= 1 (- (-> % :ret ::specs.job-queues/agents)
;;                                      (-> % :args :job-queues ::specs.job-queues/agents)))
;;                  :queued-job #(= 1 (- (-> % :ret ::specs.job-queues/jobs-waiting)
;;                                       (-> % :args :job-queues ::specs.job-queues/jobs-waiting)))
;;                  :queued-job-request#(= 1 (- (-> % :ret ::specs.job-queues/job-requests-waiting)
;;                                           (-> % :args :job-queues ::specs.job-queues/job-requests-waiting)))
;;                  :assigned-job #(= 1 (- (-> % :ret ::specs.job-queues/jobs-assigned)
;;                                         (-> % :args :job-queues ::specs.job-queues/jobs-assigned)))))
