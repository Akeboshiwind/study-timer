(ns ^:figwheel-no-load study-timer.app
  (:require [study-timer.core :as core]
            [devtools.core :as devtools]
            [clojure.spec.alpha :as s]
            [expound.alpha :as expound]))

(enable-console-print!)

(devtools/install!)

(core/init!)

(set! s/*explain-out* expound/printer)
