#!/bin/sh
curl --header "X-Api-Key: $CLOUDSMITH_API_TOKEN" https://api-prd.cloudsmith.io/v1/user/self/
