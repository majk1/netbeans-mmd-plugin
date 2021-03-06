#!/bin/bash

# Script just generates free desktop descriptor to start application

APP_HOME="$(realpath $(dirname ${BASH_SOURCE[0]}))"
TARGET=$APP_HOME/scia-reto-editor.desktop

echo [Desktop Entry] > $TARGET
echo Version=1.1 >> $TARGET
echo Encoding=UTF-8 >> $TARGET
echo Type=Application >> $TARGET
echo Name=Scia-Reto editor >> $TARGET
echo GenericName=Scia-Reto mind-map editor >> $TARGET
echo Comment=Free open source mind-map editor with PlantUML support >> $TARGET
echo Icon=$APP_HOME/icon.svg >> $TARGET
echo Exec=\"$APP_HOME/run.sh\" %f >> $TARGET
echo "Categories=Office;FlowChart;" >> $TARGET
echo "Keywords=mind;map;scia;reto;uml;plant;kstream;kafka;" >> $TARGET
echo Terminal=false >> $TARGET
echo StartupWMClass=scia.reto.editor >> $TARGET

echo Desktop script has been generated: $TARGET

if [ -d ~/.gnome/apps ]; then
    echo copy to ~/.gnome/apps
    cp -f $TARGET ~/.gnome/apps
fi

if [ -d ~/Desktop ]; then
    echo copy to ~/Desktop
    cp -f $TARGET ~/Desktop
fi

if [ -d ~/.local/share/applications ]; then
    echo copy to ~/.local/share/applications
    cp -f $TARGET ~/.local/share/applications
fi
