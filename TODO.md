# TODO:

- [ ] Re-style
- [ ] Add a Config Page
  - [ ] Save to user table
  - [ ] Configure break time
  - [ ] Configure a min study time
  - [ ] Change password
    - [ ] Only when `last_login_date` in token expiry range
- [ ] Show a retry button when fail to add time
- [ ] Fix graph size on full screen
- [ ] Flash red when at end of break
- [ ] Change the graph every tick?
- [ ] Change SQL :id to :user-id
  - [ ] Related: think about name for this in code
    - In db is :id
    - In queries is :user-id
    - In token is :user
    - Recently learned from `Elements of Clojure` that names have a `sense`, think about this
- [ ] Change :tick to use :dispatch-later
- [ ] Change all the handler functions into named anonymous functions
- [ ] Split panels into their own namespace
- [ ] New users experience
  - [ ] Add About page
  - [ ] Add a demo?
- [ ] Fix a bug where the spec fails on server error 500s
- [ ] Local cache
  - [ ] Times
  - [ ] Token
  - [ ] Offline mode?
- [ ] Token refresh
  - [ ] On request
  - [ ] Every 1 day
- [ ] Show time in title
- [ ] Change favicon based on clock state
