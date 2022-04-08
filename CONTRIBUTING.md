[Check the code of conduct here](/CODE_OF_CONDUCT.md)<br>
[General contribution and contact info](http://www.basicairdata.eu/about/)

## How a user can contribute
- In case of problems with the app, opening an issue including a complete description, the version of the app, the Android version and the device brand/model
- Opening an issue with a feature request
- Answering to other users about specific problems that the user solved
- Post ideas, graphical mockups, suggestion for the implementation of any listed issue

## How a developer can contribute
- Offering to take care of an existing issue and implement/fix it
- Proposing a new feature and follow its implementation

We suggest to ask before start coding: better to accord and discuss in detail the new feature with the maintainers before to start the implementation.

## Repository Branching Model

We are trying to follow some guidelines to keep clean and ordered this git repository tree.

Basing on https://nvie.com/posts/a-successful-git-branching-model/ (you can see a schema of it here below), we chosen to work with the following branches:

- **master** = The branch with the latest release of the application. Usually we merge the release when it has been released and we update README, Frequently Asked Question, Fastlane for the release we published. We consider origin/master to be the main branch where the source code of HEAD always reflects a production-ready state.
- **develop** = Another long life branch that goes in parallel with the master. The develop is the branch where the developers work. Usually a developer, when implements a feature, creates a new branch starting from the develop branch. When ready, the feature will be merged back into develop. We consider origin/develop to be the main branch where the source code of HEAD always reflects a state with the latest delivered development changes for the next release. Some would call this the “integration branch”. When the source code in the develop branch reaches a stable point and is ready to be released, all of the changes should be merged back into master somehow and then tagged with a release number.
- **feature branches** = Feature branches (or sometimes called topic branches) are used to develop new features for the upcoming or a distant future release. When starting development of a feature, the target release in which this feature will be incorporated may well be unknown at that point. The essence of a feature branch is that it exists as long as the feature is in development, but will eventually be merged back into develop (to definitely add the new feature to the upcoming release) or discarded (in case of a disappointing experiment). We are used to call them "issue-XXX", while associated with an issue.

We are used to include into the commit description the number of the issue (#number), in order to leave a note of the progress into the issue.

Here a schema of the base Git model we are basing on:
![alt tag](https://nvie.com/img/git-model@2x.png)
