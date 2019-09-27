(ns queues.cli-test
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.test.alpha :as stest]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]
            [midje.sweet :refer :all]
            [queues.cli :refer :all]
            [queues.init :as init]
            [queues.specs.events :as specs.events]
            [queues.test-cases :as cases]))

(let [agent (gen/generate (s/gen ::specs.events/new-agent-event))
      db (init/db)]
  (facts "processed-args"
         (fact "if receives no args returns default-options"
               (processed-args [] init/default-opts) => init/default-opts)
         (fact "if receives logging options set logging to true"
               (processed-args ["-l"] init/default-opts) => (contains {:log true})
               (processed-args ["--log"] init/default-opts) => (contains {:log true}))
         (fact "if receives pretty print options set pretty-print to true"
               (processed-args ["-p"] init/default-opts) => (contains {:pretty-print true})
               (processed-args ["--pretty-print"] init/default-opts) => (contains {:pretty-print true}))
         (fact "if receives output-file options set output-file to the file name"
               (processed-args ["-f" "file-name"] init/default-opts) => (contains {:output-file "file-name"})
               (processed-args ["--output-file" "file-name"] init/default-opts) => (contains {:output-file "file-name"}))
         (fact "if receives web server options set web-server to true"
               (processed-args ["-w"] init/default-opts) => (contains {:web-server true})
               (processed-args ["--run-web-server"] init/default-opts) => (contains {:web-server true}))
         (fact "if receives web server options set web-server to true"
               (processed-args ["-w"] init/default-opts) => (contains {:web-server true})
               (processed-args ["--run-web-server"] init/default-opts) => (contains {:web-server true}))
         (fact "if an argument is none of the above, considers it the input-file"
               (processed-args ["filename"] init/default-opts) => (contains {:input-file "filename"})))

  (facts "print-job-queue-to-console!"
         (binding [*out* (clojure.java.io/writer "stdout-simulator-1.txt")]
           (print-job-queue-to-console! cases/jobs-assigned-clj-events)
           (slurp "stdout-simulator.txt") => cases/jobs-assigned-clj-events-output-to-cli))

  (facts "log-event-res-and-jqs-to-cli-and-return-res!"
         (fact "returns result"
               (log-event-res-and-jqs-to-cli-and-return-res! agent "result" db) => "result")))

;; TODO [TEST] write test for log-event-res-and-jqs-to-cli-and-return-res!

(stest/instrument)