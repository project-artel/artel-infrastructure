folder('stage') {
    displayName('stage')
    description('Jobs that deploy to the stage environment.')
}

folder('operation') {
    displayName('operation')
    description('Jobs that deploy to the operation environment.')
}

folder('services') {
    displayName('services')
    description('Service repositories managed by GitHub multibranch pipelines.')
}

def githubCredentialsId = 'github-project-artel'
def githubOwner = 'project-artel'

def createServicePipeline = { String repositoryName ->
    multibranchPipelineJob("services/${repositoryName}") {
        displayName(repositoryName)
        description("Multibranch pipeline for ${githubOwner}/${repositoryName}. develop deploys stage, main deploys operation.")

        branchSources {
            branchSource {
                source {
                    github {
                        id("${githubOwner}-${repositoryName}")
                        repoOwner(githubOwner)
                        repository(repositoryName)
                        scanCredentialsId(githubCredentialsId)
                        configuredByUrl(false)
                        traits {
                            gitHubBranchDiscovery {
                                strategyId(1)
                            }
                            gitHubPullRequestDiscovery {
                                strategyId(1)
                            }
                            headWildcardFilter {
                                includes('develop main PR-*')
                                excludes('')
                            }
                        }
                    }
                }
            }
        }

        factory {
            workflowBranchProjectFactory {
                scriptPath('Jenkinsfile')
            }
        }

        orphanedItemStrategy {
            discardOldItems {
                numToKeep(20)
            }
        }

        triggers {
            periodicFolderTrigger {
                interval('1d')
            }
        }
    }
}

createServicePipeline('artel-agent-server')
createServicePipeline('artel-orchestration-server')
