#!/usr/bin/env bash
latest_version="$(pwd)/build/libs/$(ls -r build/libs | tr ' ' '\n' | head -n 1)"
mkdir -p dist
cp ${latest_version} dist/app.jar

cat > dist/aws-redirect-manager << 'EOF'
#!/usr/bin/env bash
exec java -jar $0 "$@"


EOF

cat dist/app.jar >> dist/aws-redirect-manager
rm dist/app.jar
chmod +x dist/aws-redirect-manager
