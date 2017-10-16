(ns ^:figwheel-no-load study-timer.app
  (:require [study-timer.core :as core]
            [devtools.core :as devtools]))

(enable-console-print!)

(devtools/install!)

(core/init!)
