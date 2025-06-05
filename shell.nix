{ pkgs ? import <nixpkgs> {} }:
pkgs.mkShell {
  packages = with pkgs; [
    jetbrains.idea-community
    temurin-bin-21
  ];
}