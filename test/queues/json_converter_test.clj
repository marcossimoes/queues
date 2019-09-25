(ns queues.json-converter-test
  (:require [clojure.spec.test.alpha :as stest]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]
            [midje.sweet :refer :all]
            [queues.fixtures :as fix]
            [queues.json-converter :refer :all]
            [queues.specs.agents :as specs.agents]
            [queues.specs.events :as specs.events]
            [queues.specs.job :as specs.job]
            [queues.specs.job-assigned :as specs.job-assigned]
            [queues.specs.job-request :as specs.job-request]
            [queues.test-cases :as cases]))

(let [clj-key-from-json-key #'queues.json-converter/clj-key-from-json-key
      clj-event-payload-from-json-type-and-payload #'queues.json-converter/clj-event-payload-from-json-type-and-payload
      clj-event-from-json-event #'queues.json-converter/clj-event-from-json-event
      clj-events-with-converted-json-event #'queues.json-converter/clj-events-with-converted-json-event
      clj-events-from-json-events #'queues.json-converter/clj-events-from-json-events
      json-events-vec-from-json-events-str #'queues.json-converter/json-events-vec-from-json-events-str
      json-key-from-clj-ns-kwd-key #'queues.json-converter/json-key-from-clj-ns-kwd-key]
  (facts "clj-key-from-json-key"
         (clj-key-from-json-key :new_agent :id) => ::specs.agents/id
         (clj-key-from-json-key :new_agent :name) => ::specs.agents/name
         (clj-key-from-json-key :new_agent :primary_skillset) => ::specs.agents/primary-skillset
         (clj-key-from-json-key :new_agent :secondary_skillset) => ::specs.agents/secondary-skillset
         (clj-key-from-json-key :new_job :id) => ::specs.job/id
         (clj-key-from-json-key :new_job :type) => ::specs.job/type
         (clj-key-from-json-key :new_job :urgent) => ::specs.job/urgent
         (clj-key-from-json-key :job_request :agent_id) => ::specs.job-request/agent-id
         (clj-key-from-json-key :new_agent) => ::specs.events/new-agent
         (clj-key-from-json-key :new_job) => ::specs.events/new-job
         (clj-key-from-json-key :job_request) => ::specs.events/job-request)
  (facts "clj-event-payload-from-json-type-and-payload"
         (clj-event-payload-from-json-type-and-payload cases/new-agent-json-event-type-p1
                                                       cases/new-agent-json-event-payload-p1) => cases/agent-p1
         (clj-event-payload-from-json-type-and-payload cases/new-job-json-type-1t
                                                       cases/new-job-json-payload-1t) => cases/job-1t
         (clj-event-payload-from-json-type-and-payload cases/job-req-json-event-type-p1
                                                       cases/job-req-json-event-payload-p1) => cases/job-req-p1)
  (facts "clj-event-from-json-event"
         (clj-event-from-json-event cases/new-agent-json-event-p1) => cases/new-agent-clj-event-p1
         (clj-event-from-json-event cases/new-job-json-event-1t) => cases/new-job-clj-event-1t
         (clj-event-from-json-event cases/job-req-json-event-p1) => cases/job-req-clj-event-p1)
  (facts "clj-event-from-json-event-str"
         (fact "if provided with a valid agent json str, returns the corresponding clj-agent"
               (clj-event-from-json-event-str cases/agent-p1-str) => cases/new-agent-clj-event-p1)
         (fact "if provided with a a mal formatted json event, throws exception"
               (clj-event-from-json-event-str cases/mal-formatted-json-event) => (throws Exception))
         (fact "if provided with a json event that has no corresponding clj event speced, throws exception"
               (clj-event-from-json-event-str cases/json-event-str-wo-respective-clj-event) => (throws Exception)))
  (facts "clj-events-with-converted-json-event"
         (clj-events-with-converted-json-event [cases/new-job-clj-event-1t] cases/new-agent-json-event-p1) => [cases/new-job-clj-event-1t cases/new-agent-clj-event-p1]
         (clj-events-with-converted-json-event [cases/new-agent-clj-event-p1] cases/new-job-json-event-1t) => [cases/new-agent-clj-event-p1 cases/new-job-clj-event-1t]
         (clj-events-with-converted-json-event [cases/new-agent-clj-event-p1] cases/job-req-json-event-p1) => [cases/new-agent-clj-event-p1 cases/job-req-clj-event-p1])
  (facts "clj-events-from-json-events"
         (clj-events-from-json-events cases/json-events) => cases/clj-events)
  (facts "json-event-from-json-event-str"
         (fact "if provided with a valid event str returns a corresponding json event"
               (json-event-from-json-event-str cases/agent-p1-str) => cases/new-agent-json-event-p1)
         (fact "if provided with an invalid event throws an exception"
               (json-event-from-json-event-str "[/]") => (throws Exception)))
  (facts "json-events-vec-from-json-events-str"
         (json-events-vec-from-json-events-str cases/json-events-str) => cases/json-events)
  (facts "clj-events-from-json-events-str"
         (clj-events-from-json-events-str cases/json-events-str) => cases/clj-events)
  (facts "json-key-from-clj-ns-kwd-key"
         (json-key-from-clj-ns-kwd-key ::specs.job-assigned/job-assigned) => "job_assigned"
         (json-key-from-clj-ns-kwd-key ::specs.job-assigned/job-id) => "job_id"
         (json-key-from-clj-ns-kwd-key ::specs.job-assigned/agent-id) => "agent_id")
  (facts "json-events-str-formatted-from-clj-events"
         (json-events-str-formatted-from-clj-events cases/jobs-assigned-clj-events) => cases/jobs-assigned-json-events-str))

(stest/instrument)