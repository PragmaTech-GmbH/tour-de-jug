# Tour de JUG

Goal: Visualize all world-wide available Java User Groups for developers to know where a JUG is. Furthermore, Speakers should be able to have a public profile to show their recent JUG Talks, also since when they speak, how many talks in total and in the future achievements (e.g. 5 talks in a row)


Open:
- How to handle miss use and speakers falsely claiming a talk at a JUG (-> let admins of JUGs approve/reject at talk)

## Personas:

Admin:

Speaker: Log in and have a profile to mark their talks.

Attendees

## Database Object Ideas

User:

- Speaker or attendees
- Log in via GitHub
- Store sign up date
- What technical identifier can we store for the user to avoid issues with hardcoding their username and the nthey change it in GitHub?

Java User Group:

- Name
- Social media link (e.g. X or LinkedIn)
- Homepage
- Meetup/Organizational Link
- Established At
- Main Contact Person(s)
- Description
- Consider JUGs can be deleted, we need a soft delete flag and a time when they went inactive

Talk Event:

- time (can also be in the future)
- link to recording (must be URL, optional)
- link to slides (must be URL, optional)
- speaker can speak multiple times at the same JUG


## Technical Hints

- Spring Boot 4 and Java 21 Backend
- Frontend: Served directly from the backend to keep it lean in the beginning
- Thyemleaf and JavaScript to use web components with Tailwind CSS
- Security: OIDC with GitHub only
- Profile page: /profiles/<slug-of-github-username>
- Start with having meaningful Jest tests for the frontend, sliced tests for the wbe layer, integration tests with Spring Boot and also full E2E with Selenide
- Deployment via Docker container and only dependency to PostgreSQL

## User Stories

Entry Page

- Page is divided 4/5 map and 1/5 left side full height search bar/list to query for Java User Groups
- The map shows pins for each Java User
- Bottom left of the sidebar: Small information text to announce e.g. new features
- Footer with link to PragmaTech and also it's imprint

Speaker View

- After log in via GitHub, speakers can create a new "Speaking Event" and use a bsic form to enter the date and time of the speaking gig, they use a input to search for a given JUG
- They can optional enter the link of the slides or recording
- They. have a profile page that shows the name of the speaker, its GitHub photo and three key metrics: total speaking gigs, speaking this year and speaker since year x
- Below is a list of their recent and upcoming speaking gigs order by time
- The profiles are public and the speaker can edit their profile


Feed content from (need to be scraped):

- https://world-wide-jugs.github.io/GlobalWWJugs/
- https://dev.java/community/jugs/

Provide a central YAML that feeds information for the list of jugs so that people can raise a PR to update content or add new user groups (we need a technical import/delta sync on Spring Boot application startup)
