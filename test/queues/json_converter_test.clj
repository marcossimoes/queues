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

(let [ns-kwd-key-from-json-key #'queues.json-converter/ns-kwd-key-from-json-key
      clj-event-payload-from-namespace-and-json-event-payload #'queues.json-converter/clj-event-payload-from-namespace-and-json-event-payload
      ns-from-js-event-type #'queues.json-converter/ns-from-js-event-type
      clj-event-payload-from-json-type-and-payload #'queues.json-converter/clj-event-payload-from-json-type-and-payload
      clj-event-from-json-event #'queues.json-converter/clj-event-from-json-event
      clj-events-with-converted-json-event #'queues.json-converter/clj-events-with-converted-json-event
      clj-events-from-json-events #'queues.json-converter/clj-events-from-json-events
      json-events-vec-from-json-events-str #'queues.json-converter/json-events-vec-from-json-events-str
      json-key-from-clj-ns-kwd-key #'queues.json-converter/json-key-from-clj-ns-kwd-key]
  (facts "ns-kwd-key-from-json-key"
         (ns-kwd-key-from-json-key "agents" "id") => ::specs.agents/id
         (ns-kwd-key-from-json-key "agents" "name") => ::specs.agents/name
         (ns-kwd-key-from-json-key "agents" "primary_skillset") => ::specs.agents/primary-skillset
         (ns-kwd-key-from-json-key "agents" "secondary_skillset") => ::specs.agents/secondary-skillset
         (ns-kwd-key-from-json-key "job" "id") => ::specs.job/id
         (ns-kwd-key-from-json-key "job" "type") => ::specs.job/type
         (ns-kwd-key-from-json-key "job" "urgent") => ::specs.job/urgent
         (ns-kwd-key-from-json-key "job-request" "agent_id") => ::specs.job-request/agent-id
         (ns-kwd-key-from-json-key "events" "new_agent") => ::specs.events/new-agent
         (ns-kwd-key-from-json-key "events" "new_job") => ::specs.events/new-job
         (ns-kwd-key-from-json-key "events" "job_request") => ::specs.events/job-request)
  (facts "clj-event-payload-from-namespace-and-json-event-payload"
         (clj-event-payload-from-namespace-and-json-event-payload "agents" cases/new-agent-json-event-payload-p1) => cases/agent-p1
         (clj-event-payload-from-namespace-and-json-event-payload "job" cases/new-job-json-payload-1t) => cases/job-1t
         (clj-event-payload-from-namespace-and-json-event-payload "job-request" cases/job-req-json-event-payload-p1) => cases/job-req-p1)
  (facts "ns-from-js-event-type"
         (ns-from-js-event-type cases/new-agent-json-event-type-p1) => "agents"
         (ns-from-js-event-type cases/new-job-json-type-1t) => "job"
         (ns-from-js-event-type cases/job-req-json-event-type-p1) => "job-request")
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
         (json-events-str-formatted-from-clj-events cases/jobs-assigned-clj-events) => cases/jobs-assigned-json-events-str)
  )

(stest/instrument)