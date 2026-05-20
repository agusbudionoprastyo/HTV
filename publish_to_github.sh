#!/bin/bash

echo "========================================="
echo "   HTV Android Project GitHub Publisher  "
echo "========================================="
echo ""

# Check if git is installed
if ! command -v git &> /dev/null
then
    echo "❌ Error: git is not installed on your system. Please install git first."
    exit 1
fi

# Initialize git if not already done
if [ ! -d ".git" ]; then
    echo "⚙️ Initializing Git repository..."
    git init
    git branch -M main
else
    echo "✓ Git repository already initialized."
fi

# Check if Git user name and email are configured
git_user=$(git config user.name)
git_email=$(git config user.email)

if [ -z "$git_user" ] || [ -z "$git_email" ]; then
    echo "👤 Git identity is not configured yet."
    echo "Please provide your details so Git can credit your commits:"
    echo ""
    read -p "Enter your Name (e.g. John Doe): " git_name
    read -p "Enter your Email (e.g. john@example.com): " git_mail
    echo ""
    
    if [ -n "$git_name" ] && [ -n "$git_mail" ]; then
        git config --global user.name "$git_name"
        git config --global user.email "$git_mail"
        echo "✓ Git identity successfully set globally!"
    else
        echo "⚠️ Name or Email was left empty. Commit might fail."
    fi
fi

# Stage files
echo "📦 Staging files for commit..."
git add .

# Check if there are changes to commit
if git diff --cached --quiet; then
    echo "✓ No new changes to commit."
else
    echo "💾 Committing files..."
    git commit -m "Initial commit - HTV Android TV App with visual optimizations"
fi

echo ""
echo "========================================="
echo "   Next Steps to Connect to GitHub:      "
echo "========================================="
echo ""
echo "1. Go to https://github.com/new and create a new repository."
echo "   (Do NOT check 'Add a README', 'Add .gitignore', or 'Choose a license')"
echo ""
echo "2. Copy the repository URL (e.g. https://github.com/username/HTV.git)"
echo ""
read -p "Enter your GitHub Repository URL: " repo_url

if [ -z "$repo_url" ]; then
    echo "⚠️ No URL entered. You can link it manually later using:"
    echo "   git remote add origin <your-repository-url>"
    echo "   git push -u origin main"
else
    # Remove existing origin if exists
    git remote remove origin 2>/dev/null
    
    # Add new origin
    git remote add origin "$repo_url"
    echo "✓ Remote origin set to: $repo_url"
    
    echo "🚀 Pushing code to GitHub..."
    git push -u origin main
    
    if [ $? -eq 0 ]; then
        echo "🎉 Successfully published to GitHub!"
    else
        echo "❌ Error: Failed to push to GitHub. Please check your credentials/internet and try again."
    fi
fi
