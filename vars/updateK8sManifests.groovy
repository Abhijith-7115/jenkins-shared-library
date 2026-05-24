#!/usr/bin/env groovy

def call(Map config = [:]) {
    def imageTag = config.imageTag ?: error("Image tag is required")
    def manifestsPath = config.manifestsPath ?: 'kubernetes'
    def gitCredentials = config.gitCredentials ?: 'github-credentials'
    def gitUserName = config.gitUserName ?: 'Jenkins CI'
    def gitUserEmail = config.gitUserEmail ?: 'jenkins@example.com'
    
    // Dynamically passed from your Jenkinsfile so it works for ANY project
    def targetRepoUrl = config.targetRepoUrl ?: error("Target Repo URL is required (e.g., github.com/your-name/repo.git)")
    def dockerUsername = config.dockerUsername ?: error("Docker username is required")
    
    echo "Updating Kubernetes manifests with image tag: ${imageTag}"
    
    withCredentials([usernamePassword(credentialsId: gitCredentials, usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_PASSWORD')]) {
        sh """
            git config user.name "${gitUserName}"
            git config user.email "${gitUserEmail}"
            
            # Dynamically update the App Deployment
            sed -i "s|image: ${dockerUsername}/eks-gitops-ecommerce-app:.*|image: ${dockerUsername}/eks-gitops-ecommerce-app:${imageTag}|g" ${manifestsPath}/08-eks-gitops-ecommerce-deployment.yaml
            
            # Dynamically update the Migration Job (if it exists)
            if [ -f "${manifestsPath}/12-eks-gitops-ecommerce-migration-job.yaml" ]; then
                sed -i "s|image: ${dockerUsername}/eks-gitops-ecommerce-migration:.*|image: ${dockerUsername}/eks-gitops-ecommerce-migration:${imageTag}|g" ${manifestsPath}/12-eks-gitops-ecommerce-migration-job.yaml
            fi
            
            # Check for changes and push
            if git diff --quiet; then
                echo "No changes to commit"
            else
                git add ${manifestsPath}/*.yaml
                git commit -m "Update image tags to ${imageTag} [ci skip]"
                
                # Push back to the repository specified in the Jenkinsfile
                git remote set-url origin https://${GIT_USERNAME}:\${GIT_PASSWORD}@\${targetRepoUrl}
                git push origin HEAD:master
            fi
        """
    }
}