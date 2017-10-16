(ns study-timer.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [study-timer.core-test]))

(doo-tests 'study-timer.core-test)

