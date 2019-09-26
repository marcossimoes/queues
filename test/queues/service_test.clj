(ns queues.service-test
  (:require [clojure.spec.test.alpha :as stest]
            [io.pedestal.test :refer [response-for]]
            [midje.sweet :refer :all]
            [queues.fixtures :as fix]
            [queues.init :as init]
            [queues.service :refer :all]
            [queues.specs.queues :as specs.queues]
            [queues.test-cases :as cases]))

(facts "get '/' endpoint"
       (fact "a simple get should return 200"
             (response-for fix/tempserv :get "/") => (contains {:status 200}))
       (fact "if db is empty, return a map with keys jobs-done, jobs-being-done and jobs-queued"
             (response-for fix/tempserv :get "/") => (contains {:body "{\n  \"jobs_done\" : [ ],\n  \"jobs_being_done\" : [ ],\n  \"jobs_queued\" : [ ]\n}"}))
       (fact "if db has jobs-done, return these jobs as jobs-done in body"
             (binding [init/*service-db* (init/db {::specs.queues/jobs-done cases/jobs-done})]
               (response-for fix/tempserv :get "/") => (contains {:body (str "{\n  \"jobs_done\" : "
                                                                             cases/jobs-done-str
                                                                             ",\n  \"jobs_being_done\" : [ ],\n  \"jobs_queued\" : [ ]\n}")})))
       (fact "if db has jobs-in-progress, return these jobs as jobs-bein-done in body"
             (binding [init/*service-db* (init/db {::specs.queues/jobs-in-progress cases/jobs-in-progress})]
               (response-for fix/tempserv :get "/") => (contains {:body (str "{\n  \"jobs_done\" : [ ],\n  \"jobs_being_done\" : "
                                                                             cases/jobs-in-progress-str
                                                                             ",\n  \"jobs_queued\" : [ ]\n}")})))
       (fact "if db has jobs-done, return these jobs as jobs-done in body"
             (binding [init/*service-db* (init/db {::specs.queues/jobs-waiting cases/jobs-waiting})]
               (response-for fix/tempserv :get "/") => (contains {:body (str "{\n  \"jobs_done\" : [ ],\n  \"jobs_being_done\" : [ ],\n  \"jobs_queued\" : "
                                                                             cases/jobs-waiting-str
                                                                             "\n}")}))))

(facts "post '/agents' endpoint"
       (fact "if no agent is provided, returns 400"
             (response-for fix/tempserv
                           :post "/agents"
                           :headers {"Content-Type" "application/json"}
                           :body "")
             => (contains {:status 400}))
       (fact "if a valid agent is provided, returns 201"
             (response-for fix/tempserv
                           :post "/agents"
                           :headers {"Content-Type" "application/json"}
                           :body "{\n   \"new_agent\": {\n     \"id\": \"8ab86c18-3fae-4804-bfd9-c3d6e8f66260\",\n     \"name\": \"BoJack Horseman\",\n     \"primary_skillset\": [\"bills-questions\"],\n     \"secondary_skillset\": []\n   }\n }\n ")
             => (contains {:status 201}))
       (fact "if a valid agent is provided, returns agent"
             (response-for fix/tempserv
                           :post "/agents"
                           :headers {"Content-Type" "application/json"}
                           :body cases/agent-p1-str)
             => (contains {:body "{\n  \"new_agent\" : {\n    \"id\" : \"8ab86c18-3fae-4804-bfd9-c3d6e8f66260\",\n    \"name\" : \"BoJack Horseman\",\n    \"primary_skillset\" : [ \"bills-questions\" ],\n    \"secondary_skillset\" : [ ]\n  }\n}"})))

(facts "post '/agents' and then get ")

;; TODO [TEST] service-test

(stest/instrument)