# Job Queue

## Brief Description

This app was developed as an answer to Nubank's queues 1 exercise.
The exercise's rubcric can be found [here](./RUBRIC.md)

THe app takes a text file with a json formatted list of customer service events,
namely a *new_agent* that has certain skillsets, a *job* that is from a certain type
(respective to the agents skillsets) and a job-request from one of the agents
stating the agent is ready to take a new job.

The app should then return a text file with a json formatted list of jobs-assigned
each one containing the agent id that received the job-assignment and the job-id from
the job that was assigned to the agent.

Given agents have a primary and a secondary skillset, and jobs have an urgent flag (true or false)
Jobs will be assigned to Agents accordingly to the following rules
1. The *first* Job that came and matches the agent's *Primary Urgent*
2. The *first* Job that came and matches the agent's *Primary*
3. The *first* Job that came and matches the agent's *Secondary Urgent*
3. The *first* Job that came and matches the agent's *Secondary*

## Usage

### System requirements

To make sure you are able to build and run the application make sure you have Java 8 or higher
(recommended: Java 8 or Java 11) and Leiningen installed and reachable in your path.

### Build and Execution

Inside queues project directory, just typ

```
lein uberjar
```

To start the application, just execute

```
    $ java -jar queues-0.1.0-standalone.jar YOUR_FILE_PATH
```

## Options

It is possible to change some behaviours of the application by passing a parameter in it'' initialization call.

| Option               | Description                                          |
|----------------------|------------------------------------------------------|
|-l, --log             | Prints logging of the apps processing                |
|-p, --pretty-print    | Turn on pretty print of the json output              |
|-f, --output-file     | Let specify the name of the output file.
                         Defaults to "jobs-assigned.json.txt" if nor provided |

TODO: include options for using the sample-input for example, specifying the output file name and pretty printing in the terminal

## Examples

There is a sample input file you can run for an example

```
    $ java -jar queues-0.1.0-standalone.jar resources/sample-input.json.txt
```

## Testing

The test suit contains a combination of unit tests, property based tests, as well as Clojure Spec s/fdef definitions
that are enabled via instrumentation when running the tests.

The example based unit tests verify happy path for specific functions in a way to quickly identify problems in
refactoring. It also verifies the program produces sample-output.json.txt after processing sample-input.json.txt

The property based tests verify that requirements of the job queue hold trye, after applying randomly generated
sequence of events to the job queue.

The test suite can be run with the following command

```
lein midje
```

```
{:result true, :num-tests 100, :seed 1562682111388, :time-elapsed-ms 782, :test-var "outputs-clj-formatted-job-assigned-agent-id-and-job-id"}
{:result true, :num-tests 100, :seed 1562682112171, :time-elapsed-ms 573, :test-var "jobs>=jobs-assigned"}
{:result true, :num-tests 100, :seed 1562682112745, :time-elapsed-ms 672, :test-var "job-requests>=jobs-assigned"}
{:result true, :num-tests 100, :seed 1562682113418, :time-elapsed-ms 573, :test-var "runs-with-out-erros-for-all-inputs"}

>>> Midje summary:
All checks (73) succeeded.

>>> Output from clojure.test tests:

Ran 4 tests containing 4 assertions.
0 failures, 0 errors.
```

### Coverage

This is the current test coverage of the app in accordance to clojure cloverage specification

|-------------------------------+---------+---------|
|                     Namespace | % Forms | % Lines |
|-------------------------------+---------+---------|
|                   queues.core |  100,00 |  100,00 |
|                   queues.json |   97,46 |   97,62 |
|           queues.models.agent |   85,19 |  100,00 |
| queues.models.agents-and-jobs |   52,77 |  100,00 |
|          queues.models.events |   81,33 |  100,00 |
|             queues.models.job |   80,65 |  100,00 |
|    queues.models.job-assigned |   53,85 |  100,00 |
|     queues.models.job-request |   85,96 |  100,00 |
|-------------------------------+---------+---------|
|                     ALL FILES |   80,04 |   99,44 |
|-------------------------------+---------+---------|


## Implementation

The job queue is implemented as a reduction of a sequence of events over an immutable, in-memory data structure.

The project was divided in two logic namespaces + 6 specs names[aces
  * *queues.core* - It is the application startup point. It is where the -main function is declared and where all the main
  business logic for processing the events is stored.
  * *queues.json* - it has functions to deal with json, including reading a json from stdin and writing to stdout
  * *queues.models.[spec-name]* - holds clojure spec for all the main data structures used in the app, namely
      * *agents-and-jobs* - map holding seqs and queues such as: agents, jobs-waiting-to-be-assigned,
      job-requests-waiting-to-be-fullfilled, jobs-assigned
      * *events* - seq of events, each event being either a new_agent to be included in agent seq in agents-and-jobs.
      a new_job to be either assigned or included in jobs-waiting queue in agents-and-jobs and job_request to be either
      assigned or included in jobs-waiting queue in agents-and-jobs.
      * *agent* - and agent with id, name, primary and secondary skillsets
      * *job* - with id, type and urgent flag
      * *job-assigned* - with job-id and agent-id
      * *job-request* with and agent-id

The application was coded in a way that all the functions containing side effects are separated in the boarders of the
application. There are used mainly to read json from stdin and write json to the stdout and called directly by the main
function. The agents-and-jobs queues are entirely managed by interactions between functions as accordingly to the
prerequisites of the exercise itself there is not need to store state in a persistent way. The application starts,
receives an input, processes it, and print an output to the stdout. Thus almost all functions in the whole application
are pure functions.

### Processing

A sequence of events can be processed into agents-and-jobs queues by using the `dequeue` function. The functions returns
a list of jobs-assigned.

```
   (-> input-file
      (slurp)
      (json/read-json-events)
      (dequeue)
      (json/write-json-events)
      (#(spit "jobs-assigned.json.txt" %)))
```

## Bugs

...