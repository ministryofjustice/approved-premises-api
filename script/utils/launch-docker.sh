#!/bin/sh

if (! docker stats --no-stream > /dev/null 2>&1  ); then
  echo "==> Launching Docker..."

  # On Mac OS this would be the terminal command to launch Docker
  systemctl --user start docker-desktop
  printf "Waiting for Docker to launch..."
 #Wait until Docker daemon is running and has completed initialisation
while (! docker stats --no-stream > /dev/null 2>&1 ); do
  # Docker takes a few seconds to initialize
  printf '.'
  sleep 1
done
fi
