#!/usr/bin/env groovy

def call(Map config = [:]) {

    def imageName = config.imageName ?: error("Image name is required")
    def imageTag = config.imageTag ?: 'latest'
    def severity = config.severity ?: 'HIGH,CRITICAL'

    echo "Running Trivy scan on ${imageName}:${imageTag}"

    sh """
        mkdir -p trivy-results

        docker run --rm \
            -v /var/run/docker.sock:/var/run/docker.sock \
            -v \$(pwd)/trivy-results:/trivy-results \
            aquasec/trivy:0.48.3 image \
            --severity ${severity} \
            --format json \
            --output /trivy-results/${imageName.replaceAll('/', '-')}-${imageTag}.json \
            ${imageName}:${imageTag}
    """

    echo "Trivy scan completed"
}
