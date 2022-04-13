# How to contribute

[Check the code of conduct here](/CODE_OF_CONDUCT.md)<br>
[General contribution and contact info](http://www.basicairdata.eu/about/)

## How a user can contribute
- In case of problems with the app, opening an issue including a complete and detailed description, the version of the app, the Android version, and the device brand/model
- Opening an issue to request a new feature or to suggest an improvement
- Answering other users about their specific problems when he already found a solution
- Posting ideas, graphical mockups, suggestion for the implementation of any listed issue
- Helping to translate the app and keep the translation updated. The users can be notified when the strings of the app are updated by subscribing the *Translation & Proofreading* channel (![issue #16](https://github.com/BasicAirData/GPSLogger/issues/16)). We use the [Crowdin](https://crowdin.com/project/gpslogger) platform to make, manage, and maintain the localizations.

## How a developer can contribute
- Offering to take care of an existing issue, discussing its details, and implementing (or fixing) it
- Proposing a new feature or improvement
- Fixing a bug
- Adjusting, formatting, commenting the code

We suggest to ask before to start coding: better to discuss all the details of the a feature with the maintainers before to start the implementation.
As a note, BasicAirData is entirely made by volunteers that develop in their spare time. Sometimes we have the time to access, answer, and push commits every day, sometimes we could be busy for some weeks; please be patient if we are not so fast to answer and to fix bugs.

## Repository Branching Model

We are trying to follow some simple guidelines to keep clean and ordered this Git repository tree.

Basing on https://nvie.com/posts/a-successful-git-branching-model/ (you can see a schema of it here below), we chosen to work with the following branches:
    
- **master** = The long life branch that contains the releases of the application. Usually we merge the release into master when it has been released and we keep updated README, Frequently Asked Question, Fastlane structure, and other documentation for the release we published. We consider origin/master to be the main branch where the source code of HEAD always reflects a production-ready state.
- **develop** = Another long life branch that goes in parallel with master. The develop is the branch where the developers work. Usually a developer, when implements a feature, creates a new branch starting from the develop branch. When ready, the feature will be merged back into develop. We consider origin/develop to be the main branch where the source code of HEAD always reflects a state with the latest delivered development changes for the next release. Some would call this the “integration branch”. When the source code in the develop branch reaches a stable point and is ready to be released, all of the changes should be merged back into master somehow and then tagged with a release number.
- **feature branches** = Feature branches (or sometimes called topic branches) are used to develop new features for the upcoming or a distant future release. Feature branches usually start from the latest commit of the develop branch and are eventually merged back into develop. When starting development of a feature, the target release in which this feature will be incorporated may well be unknown at that point. The essence of a feature branch is that it exists as long as the feature is in development, but will eventually be merged back into develop (to definitely add the new feature to the upcoming release) or discarded (in case of a disappointing experiment).<br>The feature branches are usually associated with an open issue (used for ideas, proposal, discussion, mockups, and to post the status of the implementation) and are named "issue-XXX", where XXX is the associated issue.

We are used to include the number of the issue (#number) into the description of the commit (when applicable), in order to put a note of the new progress automatically into the issue. A good comment is for example `#172 - Fix crash on Android 4 loading the Drawable`.

Here a schema of the Git model we are basing on, that shows master, develop, and feature branches:

<img src="https://nvie.com/img/git-model@2x.png" data-canonical-src="https://nvie.com/img/git-model@2x.png" width="720" />
