(ns queues.controllers.batch-processor-test
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.test.alpha :as stest]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]
            [midje.sweet :refer :all]
            [queues.controllers.batch-processor :refer :all]
            [queues.controllers.new-event-processor :refer [processed-event-with-log!]]
            [queues.init :as init]
            [queues.fixtures :as fix]
            [queues.specs.db :as specs.db]
            [queues.specs.queues :as specs.queues]
            [queues.test-cases :as cases]))

;; the let's of the tested functions bellow is necessary as this functions are private so cannot be accessed directly from the namespaces
(let [populate-db-applying-event-processor-on-batch-events! #'queues.controllers.batch-processor/populate-db-applying-event-processor-on-batch-events!
      all-job-assigned-events #'queues.controllers.batch-processor/all-job-assigned-events
      job-assigned-events-created-by-event-processor-from-new-events-batch! #'queues.controllers.batch-processor/job-assigned-events-created-by-event-processor-from-new-events-batch!
      outputs-job-assigned-json-formatted-events-to-output-options! #'queues.controllers.batch-processor/outputs-job-assigned-json-formatted-events-to-output-options!
      new-clj-events-batch-from-file #'queues.controllers.batch-processor/new-clj-events-batch-from-file]
  (facts "populate-db-applying-event-processor-on-batch-events!"
         (facts "when receives an empty vector of events"
                (let [sample-db (init/db)]
                  (populate-db-applying-event-processor-on-batch-events! sample-db
                                                                         processed-event-with-log!
                                                                         [])
                  (fact "ends with nothing to all queues"
                        (-> sample-db ::specs.db/agents deref) => cases/agents-empty
                        (-> sample-db ::specs.db/job-reqs-queued deref) => cases/job-reqs-queued-empty
                        (-> sample-db ::specs.db/jobs-waiting deref) => cases/jobs-waiting-empty
                        (-> sample-db ::specs.db/jobs-in-progress deref) => cases/jobs-in-progress-empty
                        (-> sample-db ::specs.db/jobs-done deref) => cases/jobs-done-empty)))
         (facts "when only receives a new agent event"
                (let [sample-db (init/db)]
                  (populate-db-applying-event-processor-on-batch-events! sample-db
                                                                         processed-event-with-log!
                                                                         [cases/new-agent-clj-event-p1])
                  (fact "ends with agent to agents queue"
                        (-> sample-db ::specs.db/agents deref) => {cases/agent-p1-id cases/agent-p1})
                  (fact "ends with nothing to the remaining queues"
                        (-> sample-db ::specs.db/job-reqs-queued deref) => cases/job-reqs-queued-empty
                        (-> sample-db ::specs.db/jobs-waiting deref) => cases/jobs-waiting-empty
                        (-> sample-db ::specs.db/jobs-in-progress deref) => cases/jobs-in-progress-empty
                        (-> sample-db ::specs.db/jobs-done deref) => cases/jobs-done-empty)))
         (facts "when only receives a new job event"
                (let [sample-db (init/db)]
                  (populate-db-applying-event-processor-on-batch-events! sample-db
                                                                         processed-event-with-log!
                                                                         [cases/new-job-clj-event-1t])
                  (fact "ends with job to jobs-waiting queue"
                        (-> sample-db ::specs.db/jobs-waiting deref) => [cases/job-1t])
                  (fact "ends with nothing to the remaining queues"
                        (-> sample-db ::specs.db/agents deref) => cases/agents-empty
                        (-> sample-db ::specs.db/job-reqs-queued deref) => cases/job-reqs-queued-empty
                        (-> sample-db ::specs.db/jobs-in-progress deref) => cases/jobs-in-progress-empty
                        (-> sample-db ::specs.db/jobs-done deref) => cases/jobs-done-empty)))
         (facts "when only receives a job request"
                (let [sample-db (init/db)]
                  (populate-db-applying-event-processor-on-batch-events! sample-db
                                                                         processed-event-with-log!
                                                                         [cases/job-req-clj-event-p1])
                  (fact "ends with job-request to job-requests-queued queue"
                        (-> sample-db ::specs.db/job-reqs-queued deref) => [cases/job-req-p1])
                  (fact "ends with nothing to the remaining queues"
                        (-> sample-db ::specs.db/agents deref) => cases/agents-empty
                        (-> sample-db ::specs.db/jobs-waiting deref) => cases/jobs-waiting-empty
                        (-> sample-db ::specs.db/jobs-in-progress deref) => cases/jobs-in-progress-empty
                        (-> sample-db ::specs.db/jobs-done deref) => cases/jobs-done-empty)))
         (facts "when only receives a compatible pair of a new agent and a new job but NO job-request"
                (let [sample-db (init/db)]
                  (populate-db-applying-event-processor-on-batch-events! sample-db
                                                                         processed-event-with-log!
                                                                         [cases/new-agent-clj-event-p1 cases/new-job-clj-event-1t])
                  (fact "ends with agent to agents queue"
                        (-> sample-db ::specs.db/agents deref) => {cases/agent-p1-id cases/agent-p1})
                  (fact "ends with job to jobs-waiting queue"
                        (-> sample-db ::specs.db/jobs-waiting deref) => [cases/job-1t])
                  (fact "ends with nothing to the remaining queues"
                        (-> sample-db ::specs.db/job-reqs-queued deref) => cases/job-reqs-queued-empty
                        (-> sample-db ::specs.db/jobs-in-progress deref) => cases/jobs-in-progress-empty
                        (-> sample-db ::specs.db/jobs-done deref) => cases/jobs-done-empty)))
         (facts "when receives compatible new agent, new job and job request"
                (let [sample-db (init/db)]
                  (populate-db-applying-event-processor-on-batch-events! sample-db
                                                                         processed-event-with-log!
                                                                         [cases/new-agent-clj-event-p1 cases/new-job-clj-event-1t cases/job-req-clj-event-p1])
                  (fact "ends with agent to agents queue"
                        (-> sample-db ::specs.db/agents deref) => {cases/agent-p1-id cases/agent-p1})
                  (fact "ends with job with assigned agent id to jobs-in-progress queue"
                        (-> sample-db ::specs.db/jobs-in-progress deref) => [cases/job-1t-started])
                  (fact "ends with nothing to the remaining queues"
                        (-> sample-db ::specs.db/job-reqs-queued deref) => cases/job-reqs-queued-empty
                        (-> sample-db ::specs.db/jobs-waiting deref) => cases/jobs-waiting-empty
                        (-> sample-db ::specs.db/jobs-done deref) => cases/jobs-done-empty)))
         (facts "when receives compatible new agent, new job and job request and later a job request from the same agent"
                (let [sample-db (init/db)]
                  (populate-db-applying-event-processor-on-batch-events! sample-db
                                                                         processed-event-with-log!
                                                                         [cases/new-agent-clj-event-p1 cases/new-job-clj-event-1t cases/job-req-clj-event-p1 cases/job-req-clj-event-p1])
                  (fact "ends with agent to agents queue"
                        (-> sample-db ::specs.db/agents deref) => {cases/agent-p1-id cases/agent-p1})
                  (fact "ends with last job-request to job-requests-queued queue"
                        (-> sample-db ::specs.db/job-reqs-queued deref) => [cases/job-req-p1])
                  (fact "ends with job with assigned agent id to jobs-jobs-done queue"
                        (-> sample-db ::specs.db/jobs-done deref) => [cases/job-1t-started])
                  (fact "ends with nothing to remaining-queues"
                        (-> sample-db ::specs.db/jobs-waiting deref) => cases/jobs-waiting-empty
                        (-> sample-db ::specs.db/jobs-in-progress deref) => cases/jobs-in-progress-empty)))
         (defspec min-of-job-requests-and-jobs>=jobs-started+min-of-jobs-waiting+job-reqs-waiting
                  100
                  (prop/for-all [batch-events (fix/gen-matching-events)]
                                (let [sample-db (init/db)
                                      ;; TODO [QUESTION] is this bellow a best practice?
                                      _ (populate-db-applying-event-processor-on-batch-events! sample-db
                                                                                               processed-event-with-log!
                                                                                               batch-events)
                                      num-job-reqs (fix/num-events-that batch-events fix/is-job-request?)
                                      num-jobs (fix/num-events-that batch-events fix/is-job?)
                                      num-jobs-started (+ (-> sample-db ::specs.db/jobs-in-progress deref count)
                                                          (-> sample-db ::specs.db/jobs-done deref count))
                                      num-jobs-waiting (-> sample-db ::specs.db/jobs-waiting deref count)
                                      num-job-reqs-queued (-> sample-db ::specs.db/job-reqs-queued deref count)]
                                  (>= (min num-job-reqs num-jobs)
                                      (+ num-jobs-started
                                         (min num-jobs-waiting num-job-reqs-queued))))))
         )
  (facts "all-job-assigned-events"
         (fact "if there are neither jobs in progress or done return adn empty vector"
               (let [db-empty (init/db)]
                 (all-job-assigned-events db-empty) => []))
         (fact "if there are jobs-in-progress return them as job-assigned-events"
               (let [db-data {::specs.queues/jobs-in-progress [cases/job-1t-started cases/job-2f-started]}
                     sample-db (init/db db-data)]
                 (all-job-assigned-events sample-db) => cases/jobs-assigned-clj-events))
         (fact "if there are jobs-done return them as job-assigned-events"
               (let [db-data {::specs.queues/jobs-done [cases/job-1t-started cases/job-2f-started]}
                     sample-db (init/db db-data)]
                 (all-job-assigned-events sample-db) => cases/jobs-assigned-clj-events))
         (fact "if there are jobs in progress and done consider both to create job-assigned-events"
               (let [db-data {::specs.queues/jobs-in-progress [cases/job-2f-started]
                              ::specs.queues/jobs-done        [cases/job-1t-started]}
                     sample-db (init/db db-data)]
                 (all-job-assigned-events sample-db) => cases/jobs-assigned-clj-events)))
  ;; TODO [QUESTION] the function bellow includes the processing above, should I test again for filling up the database or should I only test for outputting the job-assigned? Even the job assigned testing is it not repetitive with the all jobs-assigned function?
  (facts "job-assigned-events-created-by-event-processor-from-new-events-batch!"
         (fact "if receives an empty batch events, returns an empty vector"
               (job-assigned-events-created-by-event-processor-from-new-events-batch! (init/db)
                                                                                      processed-event-with-log!
                                                                                      [])
               => [])
         (fact "if receives matching batch agent, new-job and job-request, generates jobs-assigned"
               (job-assigned-events-created-by-event-processor-from-new-events-batch! (init/db)
                                                                                      processed-event-with-log!
                                                                                      cases/clj-events)
               => cases/jobs-assigned-clj-events))
  (facts "outputs-job-assigned-json-formatted-events-to-output-options!"
         (binding [*out* (clojure.java.io/writer "stdout-simulator-3.txt")]
           (outputs-job-assigned-json-formatted-events-to-output-options! {:pretty-print true
                                                                           :output-file  "test-of-func:-outputs-job-assigned"}
                                                                          cases/jobs-assigned-clj-events)
           (slurp "test-of-func:-outputs-job-assigned") => cases/jobs-assigned-json-events-str))
  (facts "new-clj-events-batch-from-file"
         (fact "if provided with an input file path containing a list of valid json events returns the
         corresponding clj events"
               (new-clj-events-batch-from-file "resources/sample-input.json.txt") => cases/clj-events)
         (fact "if provided with an empty file, returns an empty vector"
               (new-clj-events-batch-from-file "resources/sample-empty-file-for-test.json.txt") => []))

  ;;TODO [TEST] batch processor controller

  (stest/instrument)
  )