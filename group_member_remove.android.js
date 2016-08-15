/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 * @flow
 */


'use strict';

import React, { Component } from 'react';
import {
  AppRegistry,
  StyleSheet,
  Text,
  Image,
  ListView,
  ScrollView,
  Alert,
  TouchableHighlight,
  ActionSheetIOS,
  View
} from 'react-native';

import Spinner from 'react-native-loading-spinner-overlay';
import NavigationBar from 'react-native-navbar';
import { NativeModules } from 'react-native';


var GroupMemberRemove = React.createClass({
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
    };
  },

  removeMember: function(u) {
    console.log("remove member:", u);
    let url = this.props.url + "/groups/" + this.props.group_id + "/members/" + u.uid;
    console.log("url:", url);

    this.setState({visible:true});

    fetch(url, {
      method:"DELETE",  
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json',
        "Authorization": "Bearer " + this.props.token,
      },
    }).then((response) => {
      console.log("status:", response.status);
      if (response.status == 200) {
        this.setState({visible:false});
        this.props.eventEmitter.emit("member_removed", {id:u.uid});
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
      this.setState({visible:false});
      ToastAndroid.show('' + error, ToastAndroid.LONG)
    });
  },

  handleRemove: function() {
    console.log("confirm");
    var s = [];//selected group member
    var users = this.state.data;
    for (let i = 0; i < users.length; i++) {
      let u = users[i];
      if (u.selected) {
        s.push(u);
      }
    }

    if (s.length == 0) {
      return;
    }

    let u = s[0];
    var alertMessage = '确定要删除成员' + u.name + '?';
    Alert.alert(
      '',
      alertMessage,
      [
        {text: '取消', onPress: () => console.log('Cancel Pressed!')},
        {text: '确定', onPress: () => this.removeMember(u)},
      ]
    );
  },

  render: function() {
    console.log("render....");
    var renderRow = (rowData) => {
      var selectImage = () => {
        if (rowData.selected) {
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

    var self = this;

    var leftButtonConfig = {
      title: '取消',
      handler: function() {
        self.props.navigator.pop();
      }
    };

    var rightButtonConfig = {
      title: '删除',
      handler: this.handleRemove,
    };
    var titleConfig = {
      title: '删除成员',
    };



    return (
      <View style={{ flex: 1, backgroundColor:'#F5FCFF'}}>
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
      </View>
    );
  },

  rowPressed: function(rowData) {
    var data = this.state.data;
    var ds = this.state.dataSource;

    var newData = data.slice();

    //select only one
    for (var i = 0; i < newData.length; i++) {
      if (i != rowData.id && newData[i].selected) {
        let t = newData[i]
        let t2 = {uid:t.uid, name:t.name, id:t.id, selected:false};
        newData[i] = t2;
      }
    }
    var newRow = {uid:rowData.uid, name:rowData.name, id:rowData.id, selected:!rowData.selected};
    newData[rowData.id] = newRow;
    this.setState({data:newData, dataSource:ds.cloneWithRows(newData)});
  },

});


const styles = StyleSheet.create({
  row: {
    height:50,
  },
});

module.exports = GroupMemberRemove;


