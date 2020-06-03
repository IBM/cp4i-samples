#!/bin/bash
# Copyright 2020 IBM Corporation
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

if [ $# -ne 2 ] ; then
  echo usage: $0 app_namespace od_namespace
  echo "  app_namespace  the namespace or project in which your application is running"
  echo "  od_namespace   the namespace in which Operations Dashboard is running"
  exit 1
fi

echo Creating deployment icp4i-od-external-app-kafka-wrappers in your current project

sed -e "s/<<<<<APP_NAMESPACE>>>>>/$1/" -e "s/<<<<<OD_NAMESPACE>>>>>/$2/" templates/deployment.yaml | oc apply -f -
