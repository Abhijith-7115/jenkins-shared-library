#!/usr/bin/env groovy

def call(Map config = [:]) {
    def imageName = config.imageName ?: error("Image name is required")
    def imageTag = config.imageTag ?: 'latest'
    def severity = config.severity ?: 'HIGH,CRITICAL'
    
    echo "Running Trivy security scan on ${imageName}:${imageTag}"
    sh "mkdir -p trivy-results"
    
    def exitCode = sh(
        script: """
            docker run --rm \
                -v /var/run/docker.sock:/var/run/docker.sock \
                -v \$(pwd)/trivy-results:/trivy-results \
                aquasec/trivy:0.38.3 image \
                --format json \
                --output /trivy-results/${imageName.replaceAll('/', '-')}-${imageTag}.json \
                --severity ${severity} \
                ${imageName}:${imageTag} || true
        """,
        returnStatus: true
    )
    
    if (exitCode != 0) {
        echo "Security scan encountered issues, but continuing with the build"
    } else {
        echo "Security scan completed successfully"
    }
}
