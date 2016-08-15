'use strict';

import React, { Component } from 'react';
import {
  AppRegistry,
  StyleSheet,
  Text,
  Image,
  ListView,
  ScrollView,
  TouchableHighlight,
  ActionSheetIOS,
  ToastAndroid,
  View
} from 'react-native';

import NavigationBar from 'react-native-navbar';
import { NativeModules } from 'react-native';
import Spinner from 'react-native-loading-spinner-overlay';
var DialogAndroid = require('react-native-dialogs');


var GroupMemberAdd = React.createClass({
  getInitialState: function() {
    var rowHasChanged = function (r1, r2) {
      return r1 !== r2;
    }
    var ds = new ListView.DataSource({rowHasChanged: rowHasChanged});
    var data = this.props.users.slice();

    for (var i = 0; i < data.length; i++) {
      data[i].id = i;
    }
    return {
      data:data,
      dataSource: ds.cloneWithRows(data),
      visible:false,
    };
  },

  addMember: function(users) {
    var userIDs = [];
    for (var i = 0; i < users.length; i++) {
      userIDs.push(users[i].uid);
      users[i].member_id = users[i].uid;
    }
    
    var url = this.props.url + "/groups/" + this.props.group_id + "/members";
    this.setState({visible:true});
    fetch(url, {
      method:"POST",  
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json',
        "Authorization": "Bearer " + this.props.token,
      },
      body:JSON.stringify(userIDs),
    }).then((response) => {
      console.log("status:", response.status);
      if (response.status == 200) {
        this.setState({visible:false});
        this.props.eventEmitter.emit("member_added", {users:users});
        this.props.navigator.pop();
      } else {
        return response.json().then((responseJson)=>{
          console.log(responseJson.meta.message);
          this.setState({visible:false});
          ToastAndroid.show(responseJson.meta.message, ToastAndroid.LONG);
        });
      }
    }).catch((error) => {
      console.log("error:", error);
      ProgressHudBridge.hideTextHud('' + error);
      this.setState({visible:false});
      ToastAndroid.show('' + error, ToastAndroid.LONG)
    });
  },

  handleAdd: function() {
    var users = [];
    var data = this.state.data;
    for (var i = 0; i < data.length; i++) {
      let u = data[i];
      if (u.selected && !u.is_member) {
        users.push(u);
      }
    }
    if (users.length == 0) {
      return;
    }
    this.addMember(users);
  },

  handleCancel: function() {
    this.props.navigator.pop();
  },

  render: function() {
    var renderRow = (rowData) => {
      var selectImage = () => {
        if (rowData.is_member) {
          return require('./img/CellGraySelected.png')
        } else if (rowData.selected) {
          return  require('./img/CellBlueSelected.png');
        } else {
          return require('./img/CellNotSelected.png');
        }
      }

      return (
        <TouchableHighlight style={styles.row} onPress={() => this.rowPressed(rowData)}
                            underlayColor='#eeeeee' >
        <View style={{flexDirection:"row", flex:1, alignItems:"center" }}>
          <Image style={{marginLeft:10}} source={selectImage()}></Image>
          <Text style={{marginLeft:10}}>{rowData.name}</Text>
        </View>
      </TouchableHighlight>
      );
    }

    var leftButtonConfig = {
      title: '取消',
      handler: this.handleCancel,
    };

    var rightButtonConfig = {
      title: '确定',
      handler: this.handleAdd,
    };
    var titleConfig = {
      title: '选择联系人',
    };

    return (
      <View style={{ flex:1, backgroundColor:"#F5FCFF" }}>
          <NavigationBar
              statusBar={{hidden:true}}
              style={{}}
              title={titleConfig}
              leftButton={leftButtonConfig} 
              rightButton={rightButtonConfig} />

          <View style={{height:1, backgroundColor:"lightgrey"}}></View>

          <ListView
              dataSource={this.state.dataSource}
              renderRow={renderRow}
          />

          <Spinner visible={this.state.visible} />
        </View>
    );
  },

  rowPressed: function(rowData) {
    if (rowData.is_member) {
      return;
    }
    
    var data = this.state.data;
    var ds = this.state.dataSource;
    var newData = data.slice();
    var newRow = {uid:rowData.uid, name:rowData.name, id:rowData.id, selected:!rowData.selected, is_member:rowData.is_member};
    newData[rowData.id] = newRow;
    this.setState({data:newData, dataSource:ds.cloneWithRows(newData)});
  },

});


const styles = StyleSheet.create({
  row: {
    height:50,
  },
});


module.exports = GroupMemberAdd;

